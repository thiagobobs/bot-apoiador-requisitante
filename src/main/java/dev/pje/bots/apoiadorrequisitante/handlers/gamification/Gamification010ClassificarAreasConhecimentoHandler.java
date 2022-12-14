package dev.pje.bots.apoiadorrequisitante.handlers.gamification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.areasConhecimento.ClassificacaoAreaConhecimento;
import com.devplatform.model.bot.areasConhecimento.NivelClassificacaoAreasConhecimentoEnum;
import com.devplatform.model.bot.areasConhecimento.PontuacoesAreasConhecimento;
import com.devplatform.model.jira.custom.JiraCustomField;
import com.devplatform.model.jira.custom.JiraCustomFieldOption;

import dev.pje.bots.apoiadorrequisitante.amqp.config.AmqpProducer;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.JiraUtils;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.RocketchatMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.AtualizacaoClassificacaoAreasConhecimentoTextModel;

@Component
public class Gamification010ClassificarAreasConhecimentoHandler extends Handler<String>{
	private static final Logger logger = LoggerFactory.getLogger(Gamification010ClassificarAreasConhecimentoHandler.class);
	
	@Value("${clients.gitlab.url}")
	private String gitlabUrl;
	
	@Value("${clients.jira.user}")
	private String jiraBotUser;
	
//	@Value("${spring.rabbitmq.template.custom.gamification010-classificar-areas-conhecimento.routing-key-end}")
	private String routingKeyFinishClassification;
	
	@Override
	protected Logger getLogger() {
		return logger;
	}

//	@Autowired
	private AmqpProducer amqpProducer;

	@Override
	public String getMessagePrefix() {
		return "|GAMIFICATION||010||PRIORIDADES-AREAS-CONHECIMENTO|";
	}
	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Autowired
	private AtualizacaoClassificacaoAreasConhecimentoTextModel atualizacaoClassificacaoTextModel;

	private static long diferencaMinimaEntreAtualizacoes = 10;
	
	private String dataFinalizacaoProcesso = null;
	private String getDataFinalizacaoProcesso() {
		if(StringUtils.isBlank(dataFinalizacaoProcesso)) {
			SimpleDateFormat sdf = new SimpleDateFormat(JiraService.JIRA_DATETIME_PATTERN);
			Date now = new Date();
			dataFinalizacaoProcesso = sdf.format(now);
		}
		return dataFinalizacaoProcesso;
	}

	/**
	 * :: recalcular as pontua????es das ??reas de conhecimento ::
	 * 	- 1 vez por m??s o bot far?? uma pesquisa na base por issues pendentes em cada ??rea de conhecimento;
	 *  -- verificar a op????o: 'atualizacao' do campo, para saber se tem pelo menos 10 dias da ??ltima atualiza????o:
	 *  --- s?? recalcular se tiver mais de 10 dias
	 * 	- calcular?? o percentual de issues em cada ??rea, verificar se os n??veis foram alterados:
	 *  -- se n??o tiverem sido alterados n??o faz nada, apenas atualiza a op????o 'atualizacao' para a data atual
	 *  -- se tiver alterado alguma coisa:
	 *  --- atualiza as op????es de n??veis do campo de ctrl
	 *  --- lan??a um evento no rabbit - recalcular BV **isso permitir?? que todos os valores de BV possam ser atualizados
	 *  --- publica as novas prioridades no canal pr??prio do rocketchat
	 */
	@Override
	public void handle(String eventoRecalcularPontuacoes) throws Exception {
		messages.clean();
		// verifica qual foi a ??ltima data em que o processo foi executado - deve ter sido pelo menos a 10 dias atr??s
		String dataUltimaAtualizacao = jiraService.getDataAtualizacaoCtrlPontuacaoAreasConhecimento();
		boolean recalcularPontuacao = false;
		if(StringUtils.isEmpty(dataUltimaAtualizacao)) {
			recalcularPontuacao = true;
		}else{
			Date dataMinAtualizacao = Utils.calculateDaysFromNow(-diferencaMinimaEntreAtualizacoes);
			Date dataAtualizacao = Utils.getDateFromString(dataUltimaAtualizacao);
			recalcularPontuacao = (dataAtualizacao == null || (Utils.checkDifferenceInDaysBetweenTwoDates(dataAtualizacao, dataMinAtualizacao) <= 0));
		}
		if(recalcularPontuacao) {
			// faz uma consulta sem verificar ??rea de conhecimento para identificar a quantidade de issues ??nicas existentes
			String jqlIssuesPendentesGeral = jiraService.getJqlIssuesPendentesGeral();
			Integer totalIssuesPendentesGeral = jiraService.countIssuesFromJql(jqlIssuesPendentesGeral);
			PontuacoesAreasConhecimento pontuacoes = new PontuacoesAreasConhecimento(totalIssuesPendentesGeral);
			if(pontuacoes.getTotalIssues() != null && pontuacoes.getTotalIssues() > 0) {
				// busca o campo com todas as op????es de ??reas de conhecimento
				JiraCustomField campoAreasConhecimento = jiraService.getAreasConhecimento();
				if(campoAreasConhecimento != null && campoAreasConhecimento.getOptions() != null && !campoAreasConhecimento.getOptions().isEmpty()) {
					for (JiraCustomFieldOption areaConhecimentoOption : campoAreasConhecimento.getOptions()) {
						if(areaConhecimentoOption != null && StringUtils.isNotBlank(areaConhecimentoOption.getValue())) {
							String areaConhecimentoNome = areaConhecimentoOption.getValue();
							// para cada op????o de ??rea de conhecimento executa a consulta
							String jqlIssuesAreaConhecimento = jiraService.getJqlIssuesPendentesGeralPorAreaConhecimento(areaConhecimentoNome);
							Integer totalIssuesAreaConhecimento = jiraService.countIssuesFromJql(jqlIssuesAreaConhecimento);
							Double percentIssueFromTotal = ((double)totalIssuesAreaConhecimento / (double)totalIssuesPendentesGeral);
							messages.info("Demandas ??rea : " + areaConhecimentoNome + " - " + totalIssuesAreaConhecimento + " - " + Utils.doubleToStringAsPercent(percentIssueFromTotal));
							ClassificacaoAreaConhecimento classificacaoAC 
								= new ClassificacaoAreaConhecimento(areaConhecimentoNome, totalIssuesAreaConhecimento, percentIssueFromTotal);
							pontuacoes.addClassificacaoAreasConhecimento(classificacaoAC);
						}
					}
					pontuacoes = categorizarAreasConhecimentoPorRelevancia(pontuacoes);
					// busca o campo de controle
					JiraCustomField ctrlAreasConhecimento = jiraService.getCtrlPontuacaoAreasConhecimento();
					// verifica se as ??reas de conhecimento mudaram de n??vel
					if(!comparaCampoCtrlComPontuacoes(ctrlAreasConhecimento, pontuacoes)) {
						// atualizar o campo de ctrl para as novas pontuacoes

						for (NivelClassificacaoAreasConhecimentoEnum nivel : NivelClassificacaoAreasConhecimentoEnum.values()) {
							JiraCustomFieldOption option = JiraUtils.getOptionWithName(ctrlAreasConhecimento.getOptions(), nivel.name(), false);
							if(option == null) {
								jiraService.createCustomFieldOption(nivel.name(), null, ctrlAreasConhecimento);
								ctrlAreasConhecimento = jiraService.getCtrlPontuacaoAreasConhecimento();
								option = JiraUtils.getOptionWithName(ctrlAreasConhecimento.getOptions(), nivel.name(), false);
							}
							List<String> areasConhecimentoNivel = getAreasConhecimentoDeNivel(pontuacoes.getClassificacoesAreasConhecimento(), nivel.toString());
							List<JiraCustomFieldOption> cascadingOptions = new ArrayList<>();
							if(areasConhecimentoNivel != null) {
								int seq = 1;
								for (String areaConhecimento : areasConhecimentoNivel) {
									JiraCustomFieldOption cascadingOption = new JiraCustomFieldOption();
									cascadingOption.setValue(areaConhecimento);
									cascadingOption.setSequence(seq++);
									cascadingOptions.add(cascadingOption);
								}
							}
							option.setCascadingOptions(cascadingOptions);
							
							jiraService.updateCustomFieldOption(ctrlAreasConhecimento.getId().toString(), option.getId().toString(), option);
							messages.info("Atualizou a op????o: " + option.getValue());
						}

						// publica as novas prioridades no canal pr??prio do rocketchat
						atualizacaoClassificacaoTextModel.setPontuacoes(pontuacoes);
						atualizacaoClassificacaoTextModel.setDataAtualizacaoClassificacao(getDataFinalizacaoProcesso());
						atualizacaoClassificacaoTextModel.setJqlBase(jiraService.getJqlIssuesPendentesGeral());
						
						RocketchatMarkdown rocketMarkdown = new RocketchatMarkdown();
						String rocketAtualizacaoClassificacaoText = atualizacaoClassificacaoTextModel.convert(rocketMarkdown);
						rocketchatService.sendBotMessage(rocketAtualizacaoClassificacaoText);
						rocketchatService.sendMessagePlataformaPJEDev(rocketAtualizacaoClassificacaoText, false);						

						/**
						 * Atualiza????o da classifica????o das ??reas de conhecimento em: DD/MM/YYYY
						 * Total de issues avaliadas: XX >> link para as issues
						 * N??vel 1 >> link para as issues
						 * 	- ??rea X - num issues (N%) >> link para as issues
						 *  - ??rea Y - num issues (M%) >> link para as issues
						 * N??vel 2
						 * 	- ??rea Z - num (%)
						 * N??vel 3
						 * 	- Area 
						 * ....
						 */
						// lan??a um evento no rabbit - recalcular BV **isso permitir?? que todos os valores de BV possam ser atualizados
						String msg = "Finalizada a reclassifica????o das ??reas de conehcimento em :: " + getDataFinalizacaoProcesso();
						messages.info(msg);
						amqpProducer.sendMessageGeneric(msg, routingKeyFinishClassification);
					}
					// atualiza a opcao 'atualizacao' com a data de execu????o desta operacao
					JiraCustomFieldOption option = JiraUtils.getOptionWithName(ctrlAreasConhecimento.getOptions(), JiraService.CTRL_PONTUACAO_AREA_CONHECIMENTO_DATA_ATUALIZACAO_OPTION, false);
					if(option == null) {
						jiraService.createCustomFieldOption(JiraService.CTRL_PONTUACAO_AREA_CONHECIMENTO_DATA_ATUALIZACAO_OPTION, null, ctrlAreasConhecimento);
						ctrlAreasConhecimento = jiraService.getCtrlPontuacaoAreasConhecimento();
						option = JiraUtils.getOptionWithName(ctrlAreasConhecimento.getOptions(), JiraService.CTRL_PONTUACAO_AREA_CONHECIMENTO_DATA_ATUALIZACAO_OPTION, false);
					}
					if(option == null) {
						messages.error("Falhou ao tentar recuperar a nova opcao: " + JiraService.CTRL_PONTUACAO_AREA_CONHECIMENTO_DATA_ATUALIZACAO_OPTION);
					}else {
						List<JiraCustomFieldOption> cascadingOptions = new ArrayList<>();
						JiraCustomFieldOption dataAtualizacaoOption = new JiraCustomFieldOption();
						dataAtualizacaoOption.setValue(getDataFinalizacaoProcesso());
						cascadingOptions.add(dataAtualizacaoOption);
						option.setCascadingOptions(cascadingOptions);					
						jiraService.updateCustomFieldOption(ctrlAreasConhecimento.getId().toString(), option.getId().toString(), option);
					}
				}
			}
		}
	}
	
	/**
	 * 	-- ??rea de conhecimento (por ordem decrescente de n??mero de issues pendentes) - o total de issues pendentes por ??rea de conhecimento deve ser calculado 1 vez por m??s
	 * --- n??vel1 - ??reas com at?? 20%
	 * --- n??vel2 - ??reas entre 20% e 45% 
	 * --- n??vel3 - ??reas entre 45% e 75%
	 * --- n??vel4 - ??reas entre 75% e 90%
	 * --- n??vel5 - ??reas acima de 90%
	 * **Se nenhuma ??rea atingir o limite de X% do n??vel, pega-se a ??rea que chegou mais pr??ximo do objetivo
	 */
	private static double limiteNivel1 = 0.20;
	private static double limiteNivel2 = 0.45;
	private static double limiteNivel3 = 0.80;
	private static double limiteNivel4 = 0.98;
	
	public PontuacoesAreasConhecimento categorizarAreasConhecimentoPorRelevancia(PontuacoesAreasConhecimento pontuacoes) {
		if(pontuacoes != null && pontuacoes.getClassificacoesAreasConhecimento() != null) {
			List<ClassificacaoAreaConhecimento> classificacoes = pontuacoes.getClassificacoesAreasConhecimento();
			List<ClassificacaoAreaConhecimento> classificacoesCategorizadas = new ArrayList<>();
			Collections.sort(classificacoes, new SortTotalIssuesAreasConhecimento());
			
			int ordem = 1;
			Double totalCobertura = 0.0;
			boolean possuiNivel1 = false;
			boolean possuiNivel2 = false;
			boolean possuiNivel3 = false;
			boolean possuiNivel4 = false;
			for (ClassificacaoAreaConhecimento classificacao : classificacoes) {
				classificacao.setOrdem(ordem++);
				classificacao.setNivel(NivelClassificacaoAreasConhecimentoEnum.NIVEL5);
				if(classificacao.getTotalIssues() != null && classificacao.getTotalIssues() > 0) {
					totalCobertura += classificacao.getPercentual();
					if(!possuiNivel1 || totalCobertura < limiteNivel1) {
						classificacao.setNivel(NivelClassificacaoAreasConhecimentoEnum.NIVEL1);
						possuiNivel1 = true;
					}else if(!possuiNivel2 || totalCobertura < limiteNivel2) {
						classificacao.setNivel(NivelClassificacaoAreasConhecimentoEnum.NIVEL2);
						possuiNivel2 = true;
					}else if(!possuiNivel3 || totalCobertura < limiteNivel3) {
						classificacao.setNivel(NivelClassificacaoAreasConhecimentoEnum.NIVEL3);
						possuiNivel3 = true;
					}else if(!possuiNivel4 || totalCobertura < limiteNivel4) {
						classificacao.setNivel(NivelClassificacaoAreasConhecimentoEnum.NIVEL4);
						possuiNivel4 = true;
					}
				}
				classificacoesCategorizadas.add(classificacao);
			}
			pontuacoes.setClassificacoesAreasConhecimento(classificacoesCategorizadas);
		}
		return pontuacoes;
	}
	
	class SortTotalIssuesAreasConhecimento implements Comparator<ClassificacaoAreaConhecimento>{
	    public int compare(ClassificacaoAreaConhecimento a, ClassificacaoAreaConhecimento b)
	    { 
	    	int diff = (-1) * (a.getTotalIssues() - b.getTotalIssues()); // points in reverse order
	    	if(diff == 0 && StringUtils.isNotBlank(a.getNome()) && StringUtils.isNotBlank(b.getNome())) {
	    		diff = a.getNome().compareToIgnoreCase(b.getNome()); // names in ascending order
	    	}
	        return diff; 
	    } 
	}
	
	/**
	 * Compara o campo de controle com as pontuacoes, se houver diferenca de classificacao retorna false, se tudo estiver igual retorna true
	 * @param ctrlAreasConhecimento
	 * @param pontuacoes
	 * @return
	 */
	private Boolean comparaCampoCtrlComPontuacoes(JiraCustomField ctrlAreasConhecimento, PontuacoesAreasConhecimento pontuacoes) {
		Boolean conjuntosIguais = false;
		if(ctrlAreasConhecimento != null && ctrlAreasConhecimento.getOptions() != null 
				&& pontuacoes != null && pontuacoes.getClassificacoesAreasConhecimento() != null) {
			conjuntosIguais = true;
			for (NivelClassificacaoAreasConhecimentoEnum nivel : NivelClassificacaoAreasConhecimentoEnum.values()) {
				List<JiraCustomFieldOption> options = JiraUtils.getChildrenOptionsFromOptionName(ctrlAreasConhecimento.getOptions(), nivel.name());
				List<String> areasConhecimentoNivel = getAreasConhecimentoDeNivel(pontuacoes.getClassificacoesAreasConhecimento(), nivel.toString());
				if(!JiraUtils.compareOptionsAndNames(options, areasConhecimentoNivel)) {
					conjuntosIguais = false;
					break;
				}
			}
		}
		return conjuntosIguais;
	}
	
	private List<String> getAreasConhecimentoDeNivel(List<ClassificacaoAreaConhecimento> areasConhecimento, String nivel){
		List<String> areasConhecimentoNivel = new ArrayList<>();
		if(areasConhecimento != null && nivel != null) {
			for (ClassificacaoAreaConhecimento areaConhecimento : areasConhecimento) {
				NivelClassificacaoAreasConhecimentoEnum nivelAtual = areaConhecimento.getNivel();
				if(nivelAtual != null && nivel.equals(nivelAtual.toString())) {
					areasConhecimentoNivel.add(areaConhecimento.getNome());
				}
			}
		}
		return areasConhecimentoNivel;
	}	
}