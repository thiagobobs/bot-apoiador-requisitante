package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.approvals.TipoPermissaoMREnum;
import com.devplatform.model.gitlab.GitlabEventChangedItem;
import com.devplatform.model.gitlab.GitlabLabel;
import com.devplatform.model.gitlab.GitlabMergeRequestActionsEnum;
import com.devplatform.model.gitlab.GitlabMergeRequestAttributes;
import com.devplatform.model.gitlab.GitlabMergeRequestStateEnum;
import com.devplatform.model.gitlab.GitlabMergeRequestStatusEnum;
import com.devplatform.model.gitlab.GitlabUser;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.JiraIssueFieldOption;
import com.devplatform.model.jira.JiraUser;

import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.GitlabService;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.GitlabUtils;
import dev.pje.bots.apoiadorrequisitante.utils.JiraUtils;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.GitlabMarkdown;

@Component
public class LabelsChangeEventHandler extends Handler<GitlabEventMergeRequest>{

	private static final Logger logger = LoggerFactory.getLogger(LabelsChangeEventHandler.class);

	@Value("${clients.gitlab.url}")
	private String gitlabUrl;

	@Value("${clients.jira.url}")
	private String jiraUrl;

	@Value("${clients.jira.user}")
	private String jiraBotUser;

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "[GITLAB]-[MERGE-REQUEST]-[CHANGE-LABELS]";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	/***
	 * Monitora as labels de aprova????o de MRs do GIT
	 * 
	 * 1- Usu??rio n??o poder?? excluir label(s).
	 * 
	 * 2- Usu??rio n??o poder?? incluir mais de um label.
	 * 
	 * 3- Usu??rio somente poder?? incluir label de aprova????o se:
	 * 		3.1- Pertencer ao grupo revisor no Jira (PJE_RevisoresDeCodigo).
	 * 		3.2- Possuir email diferente do usu??rio que realizou o commit.
	 * 		3.3- A label pertencer ao tribunal em que est?? lotado.
	 */
	@Override
	public void handle(GitlabEventMergeRequest event) {
		messages.clean();

		String email = event.getUser().getEmail().equals("[REDACTED]") ? gitlabService.findUserByUsername(event.getUser().getUsername()).getEmail() : event.getUser().getEmail();

		if (!email.equals("bot.revisor.pje@cnj.jus.br") && GitlabMergeRequestActionsEnum.UPDATE.equals(event.getObjectAttributes().getAction()) 
				&& event.getChanges() != null && event.getChanges().getLabels() != null) {

			GitlabEventChangedItem<List<GitlabLabel>> changedLabels = event.getChanges().getLabels();
			
			if (!changedLabels.getRemovedItems().isEmpty()) {
				this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), 
						"Usu??rio " + email + " n??o possui permiss??o para a excluir label(s).");
				
				this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), GitlabUtils.translateGitlabLabelListToValueList(event.getChanges().getLabels().getPrevious()));
			} else {
				List<GitlabLabel> addedItemsLabel = changedLabels.getAddedItems();
				
				if (addedItemsLabel.size() > 1) {
					this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), 
							"Usu??rio " + email + " n??o possui permiss??o para a incluir mais de 1 (um) label.");
					
					this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), GitlabUtils.translateGitlabLabelListToValueList(event.getChanges().getLabels().getPrevious()));
				} else {
					JiraUser jiraUser = jiraService.findUserByUserName(email);
					
					if (!jiraService.isRevisorCodigo(jiraUser)) {
						this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), 
								"Usu??rio " + email + " n??o possui permiss??o para adicionar label de aprova????o.");
						
						this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), GitlabUtils.translateGitlabLabelListToValueList(event.getChanges().getLabels().getPrevious()));
					} else {
						if (event.getObjectAttributes().getLastCommit().getAuthor().getEmail().equals(jiraUser.getEmailAddress())) {
							this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), 
									"Usu??rio " + email + " n??o possui permiss??o para aprovar o pr??prio c??digo.");
							
							this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), GitlabUtils.translateGitlabLabelListToValueList(event.getChanges().getLabels().getPrevious()));
						} else {
							String tribunalUsuario = jiraService.getTribunalUsuario(jiraUser, false);
							if (tribunalUsuario == null || !addedItemsLabel.get(0).getTitle().contains(tribunalUsuario)) {
								this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), 
										"Usu??rio " + email + " n??o possui permiss??o para incluir label de aprova????o de outro tribunal que n??o o seu.");
								
								this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), GitlabUtils.translateGitlabLabelListToValueList(event.getChanges().getLabels().getPrevious()));
							} else {
								JiraIssue issue = this.jiraService.getIssue(event);
								
								List<JiraUser> responsaveisRevisao = issue.getFields().getResponsaveisRevisao();
								responsaveisRevisao.add(jiraUser);

								Map<String, Object> updateFields = new HashMap<>();
								this.jiraService.atualizarResponsaveisRevisao(issue, responsaveisRevisao, updateFields);
								this.jiraService.adicionarComentario(issue, "MR#" + event.getObjectAttributes().getIid() +" aprovado pelo usu??rio " + jiraUser.getEmailAddress(), updateFields);

								this.enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_EDICAO_AVANCADA, false, false);
								
								List<String> labelsAprovacao = recuperaLabelsAprovacao(GitlabUtils.translateGitlabLabelListToValueList(event.getLabels()));
								
								logger.info("Issue type: {} | Labels aprova????o: {}", issue.getFields().getIssuetype().getName(), labelsAprovacao);
								
								if (("Defeito".equals(issue.getFields().getIssuetype().getName()) && labelsAprovacao.size() == 1) || 
										("Melhoria".equals(issue.getFields().getIssuetype().getName()) && labelsAprovacao.size() == 2) || 
											labelsAprovacao.size() >= 3) {

									try {
										this.gitlabService.rebaseMergeRequest(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
									} catch (GitlabException ex) {
										this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), ex.getLocalizedMessage());
										this.gitlabService.closeMergeRequest(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
									}
								}
							}
						}
					}
				}
			}
		}
//			
//			
//
//			
//						
//			List<String> labelsAdicionadas = GitlabUtils.translateGitlabLabelListToValueList(addedItemsLabel);
//			
//			
//			
//			
//			List<String> labelsAprovacoesAdicionadas = recuperaLabelsAprovacao(GitlabUtils.translateGitlabLabelListToValueList(addedItemsLabel));
//			List<String> labelsAprovacoesRemovidas = recuperaLabelsAprovacao(GitlabUtils.translateGitlabLabelListToValueList(removedItemsLabel));
//
//			if((labelsAprovacoesAdicionadas != null && !labelsAprovacoesAdicionadas.isEmpty()) ||
//					(labelsAprovacoesRemovidas != null && !labelsAprovacoesRemovidas.isEmpty())) {
//				String gitlabProjectId = gitlabEventMR.getProject().getId().toString();
//				BigDecimal mrIID = gitlabEventMR.getObjectAttributes().getIid();
//				
// 				GitlabMRResponse mergeRequest = gitlabService.getMergeRequest(gitlabProjectId, mrIID);
//				JiraIssue issue = jiraService.getIssue(gitlabEventMR);
//				GitlabUser revisorGitlab = gitlabEventMR.getUser();
//				JiraUser revisorJira = jiraService.getJiraUserFromGitlabUser(revisorGitlab);
//				String tribunalUsuarioRevisor = jiraService.getTribunalUsuario(revisorJira, false);
//				
//				// verifica se o usu??rio tem permissao de alterar a label
//				// retorno: tem permissao / n??o tem - mesmo autor do commit / n??o tem - resposponsavel pela codificacao da issue / n??o tem - alterou label errada
//				TipoPermissaoMREnum permissaoUsuario = verificaPermissaoUsuario(revisorJira, revisorGitlab, tribunalUsuarioRevisor, 
//						issue, mergeRequest, labelsAprovacoesAdicionadas, labelsAprovacoesRemovidas);
//				
//				if(permissaoUsuario != null && permissaoUsuario.equals(TipoPermissaoMREnum.COM_PERMISSAO)) {
//					messages.info("O usu??rio " + revisorJira.getName() + " possui permiss??o para a altera????o de labels:\n" + changedLabels);
//					// identifica os dados da issue e do merge
//					boolean adicaoDeLabel = (labelsAprovacoesAdicionadas != null && !labelsAprovacoesAdicionadas.isEmpty());
//					List<JiraUser> responsaveisRevisao = issue.getFields().getResponsaveisRevisao();
//					List<JiraIssueFieldOption> tribunaisResponsaveisRevisao = issue.getFields().getTribunaisResponsaveisRevisao();
//					List<String> tribunaisRevisoresIssue = JiraUtils.translateFieldOptionsToValueList(tribunaisResponsaveisRevisao);
//					
//					/**
//					 * se nao tiver sido aprovado por um usu??rio de servi??o, atualiza:
//					 * - a lista de pessoas revisoras da issue
//					 * - a lista de tribunais revisores da issue
//					 */
//					if(!jiraService.isServico(revisorJira)) {
//						// indica na issue quem ?? o respons??vel pela aprova????o - na verdade adiciona mais um nome aos j?? existentes
//						responsaveisRevisao = atualizaListaUsuariosRevisores(adicaoDeLabel, responsaveisRevisao, revisorJira);
//						// indica na issue qual tribunal ?? o respons??vel pela aprova????o - de acordo com o respons??vel pela aprova????o
//						tribunaisRevisoresIssue = atualizaListaTribunaisRevisores(adicaoDeLabel, tribunaisRevisoresIssue, tribunalUsuarioRevisor);
//					}
//					
//					List<String> labelsList = new ArrayList<>();
//					if(mergeRequest != null) {
//						labelsList = mergeRequest.getLabels();
//					}
//					/**
//					 * - se houver mais tribunais nas labels do que na issue
//					 * -- retira a label do MR, pois n??o tem como saber quem fez a a????o, indica que houve erro na opera????o
//					 */
//					List<String> labelsAprovacoesTribunais = recuperaLabelsAprovacao(labelsList);
//					try {
//						removeLabelsSemTribunalRevisorNaIssue(mergeRequest, issue, labelsAprovacoesTribunais, tribunaisRevisoresIssue, labelsList);
//					}catch (Exception e) {
//						String errorMsg = "Houve um problema ao tentar remover as labels sem tribunal revisor do MR - erro: " + e.getLocalizedMessage();
//						messages.error(errorMsg);
//					}
//					
//					/**
//					 * - se houver mais tribunais na issue do que nas labels
//					 * -- retira o tribunal da issue
//					 */
//					if(tribunaisRevisoresIssue != null) {
//						List<String> tribunaisAprovadoresSemLabel = identificaTribunalRevisorSemLabelAprovacao(labelsAprovacoesTribunais, tribunaisRevisoresIssue);
//						// retirar da lista de tribunais revisores os tribunais sem label
//						tribunaisRevisoresIssue.removeAll(tribunaisAprovadoresSemLabel);
//						// retirar da lista de usu??rios revisores os usu??rios dos tribunais sem label 
//						responsaveisRevisao = removeUsuariosDosTribunais(responsaveisRevisao, tribunaisAprovadoresSemLabel);
//					}
//					
//					// faz a recontagem de quantos tribunais aprovaram e preenche o campo: "Aprova????es realizadas"
//					Integer aprovacoesRealizadas = (tribunaisRevisoresIssue != null ? tribunaisRevisoresIssue.size() : 0);
//					// manda mensagem no jira / canais dos tribunais requisitantes / canal do grupo revisor
//					// se j?? completou todas as aprova????es, aprova o MR
//					
//					Integer aprovacoesNecessarias = issue.getFields().getAprovacoesNecessarias();
//					List<JiraIssueFieldOption> tribunaisRequisitantes = issue.getFields().getTribunalRequisitante();
//					
//					List<String> listaTribunaisRequisitantesIssue = JiraUtils.translateFieldOptionsToValueList(tribunaisRequisitantes);
//					List<String> listaTribunaisRequisitantesPendentesAprovacao = Utils.getValuesOnlyInA(listaTribunaisRequisitantesIssue, tribunaisRevisoresIssue);
//					
//					boolean houveAlteracoesNasAprovacoes = verificaSeHouveAlteracaoRevisores(responsaveisRevisao, tribunaisRevisoresIssue, issue);
//					Map<String, Object> updateFields = new HashMap<>();
//					
//					// aprovacoes realizadas
//					jiraService.atualizarAprovacoesRealizadas(issue, aprovacoesRealizadas, updateFields);
//					// usuarios responsaveis aprovacoes
//					jiraService.atualizarResponsaveisRevisao(issue, responsaveisRevisao, updateFields);
//					// atualiza tribunais responsaveis aprovacoes
//					jiraService.atualizarTribunaisRevisores(issue, tribunaisRevisoresIssue, updateFields);
//					
//					// adiciona os MR abertos
//					String MRsAbertos = issue.getFields().getMrAbertos();
//					String urlMR = gitlabEventMR.getObjectAttributes().getUrl();
//					MRsAbertos = Utils.concatenaItensUnicosStrings(MRsAbertos, urlMR);
//					
//					String MrsAbertosConfirmados = gitlabService.checkMRsOpened(MRsAbertos);
//					jiraService.atualizarMRsAbertos(issue, MrsAbertosConfirmados, updateFields, true);
//					
//					aprovacoesMRTextModel.setAprovacoesNecessarias(aprovacoesNecessarias);
//					aprovacoesMRTextModel.setAprovacoesRealizadas(aprovacoesRealizadas);
//					aprovacoesMRTextModel.setAprovou(adicaoDeLabel);
//					aprovacoesMRTextModel.setIssue(issue);
//					aprovacoesMRTextModel.setMergeRequest(gitlabEventMR.getObjectAttributes());
//					aprovacoesMRTextModel.setUltimoRevisor(revisorJira);
//					aprovacoesMRTextModel.setUsuariosResponsaveisAprovacoes(responsaveisRevisao);
//					aprovacoesMRTextModel.setTribunaisRequisitantesPendentes(listaTribunaisRequisitantesPendentesAprovacao);
//
//					// se o usu??rio n??o ?? de servi??o - publica a altear????o como um coment??rio na issue
//					if(!jiraService.isServico(revisorJira)) {
//						if(houveAlteracoesNasAprovacoes) {
//							// adiciona o coment??rio
//							JiraMarkdown jiraMarkdown = new JiraMarkdown();
//							String aprovacoesMRTextJira = aprovacoesMRTextModel.convert(jiraMarkdown);
//							jiraService.adicionarComentario(issue, aprovacoesMRTextJira, updateFields);
//						}
//					}
//					
//					if(updateFields != null && !updateFields.isEmpty()) {
//						enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_EDICAO_AVANCADA, false, false);
//					}
//					
//					if(!jiraService.isServico(revisorJira)) {
//						if(houveAlteracoesNasAprovacoes) {
//							RocketchatMarkdown rocketchatMarkdown = new RocketchatMarkdown();
//							String aprovacoesMRTextRocketchat = aprovacoesMRTextModel.convert(rocketchatMarkdown);
//							
//							// envia mensagens para os tribunais requisitantes da demanda
//							rocketchatService.sendMessageCanaisEspecificos(aprovacoesMRTextRocketchat, listaTribunaisRequisitantesIssue, false);
//							// envia mensagens para o grupo revisor
//							rocketchatService.sendMessageGrupoRevisorTecnico(aprovacoesMRTextRocketchat);
//							
//							// envia para o MR relacionado
//							GitlabMarkdown gitlabMarkdown = new GitlabMarkdown();
//							String aprovacoesMRTextGitlab = aprovacoesMRTextModel.convert(gitlabMarkdown);
//							gitlabService.sendMergeRequestComment(gitlabProjectId, mrIID, aprovacoesMRTextGitlab);
//						}
//					}
//					if(aprovacoesRealizadas >= aprovacoesNecessarias && gitlabEventMR.getObjectAttributes() != null) {
//						// aprovar o MR
//						GitlabMRResponse response = gitlabService.acceptMergeRequest(gitlabProjectId, mrIID);
//						if(response == null) {
//							messages.error("Houve um erro ao tentar aceitar o MR: "+ mrIID + " - do projeto: " + gitlabProjectId);
//						}
//					}
//					
//				}else {
//					messages.error("O usu??rio " + (revisorJira != null ? revisorJira.getName() : "NULL") + " n??o possui permiss??o para alterar uma ou mais labels do MR, revertendo altera????es.");
//					
//					// reverter a altera????o indicando o motivo:
//					/**
//					 * - usu??rio n??o ?? um revisor habilitado
//					 * - usu??rio ?? o autor do ??ltimo commit do branch
//					 * - usu??rio ?? o respons??vel pela codifica????o da issue
//					 */
//					List<GitlabLabel> labelsAnteriores = changedLabels.getPrevious();
//					List<String> labels = new ArrayList<>();
//					for (GitlabLabel label : labelsAnteriores) {
//						labels.add(label.getTitle());
//					}
//					
//					GitlabMRResponse response = gitlabService.atualizaLabelsMR(gitlabEventMR.getObjectAttributes(), labels);
//					if(response == null) {
//						messages.error("Houve um erro ao tentar reverter a altear????o nas labels do MR :: MRIID=" + mrIID 
//								+ " - labels:\n" + labels);
//					}else {
//						StringBuilder mensagemRevertLabels = new StringBuilder();
//						GitlabMarkdown gitlabMarkdown = new GitlabMarkdown();
//						String issueLink = JiraUtils.getPathIssue(issue.getKey(), jiraUrl);
//						
//						mensagemRevertLabels
//						.append(gitlabMarkdown.normal("As labels: "))
//						.append(gitlabMarkdown.normal(String.join(", ", labels)))
//						.append(gitlabMarkdown.normal(" foram reestabelecidas, pois o usu??rio"));
//						String motivoReversao = "n??o tem permiss??o para alterar uma ou todas as labels alteradas";
//						if(permissaoUsuario == null || permissaoUsuario.equals(TipoPermissaoMREnum.SEM_PERMISSAO_NAO_EH_REVISOR)) {
//							motivoReversao = "n??o ?? um revisor de c??digo registrado";
//						}else if(permissaoUsuario.equals(TipoPermissaoMREnum.SEM_PERMISSAO_AUTOR_COMMIT)) {
//							motivoReversao = "?? o autor do ??ltimo commit";
//						}else if(permissaoUsuario.equals(TipoPermissaoMREnum.SEM_PERMISSAO_RESPONSAVEL_CODIFICACAO)) {
//							motivoReversao = "?? o respons??vel pela codifica????o na issue " + gitlabMarkdown.link(issueLink, issue.getKey());
//						}
//						mensagemRevertLabels
//						.append(" ")
//						.append(gitlabMarkdown.bold(motivoReversao.trim()))
//						.append(".");
//						gitlabService.sendMergeRequestComment(gitlabProjectId, mrIID, mensagemRevertLabels.toString());
//					}
//				}
//			}
//			
//		}
	}
	
	private boolean verificaSeHouveAlteracaoRevisores(List<JiraUser> responsaveisRevisaoAtualizado, List<String> tribunaisRevisoresAtualizado, JiraIssue issue) {
		List<JiraUser> responsaveisRevisaoAnteriores = issue.getFields().getResponsaveisRevisao();
		List<JiraIssueFieldOption> tribunaisResponsaveisRevisaoAnteriores = issue.getFields().getTribunaisResponsaveisRevisao();
		List<String> tribunaisRevisoresAnteriores = JiraUtils.translateFieldOptionsToValueList(tribunaisResponsaveisRevisaoAnteriores);
		
		boolean houveAlteracao = false;
		if(responsaveisRevisaoAnteriores == null) {
			responsaveisRevisaoAnteriores = new ArrayList<>();
		}
		if(responsaveisRevisaoAtualizado == null) {
			responsaveisRevisaoAtualizado = new ArrayList<>();
		}
		if(responsaveisRevisaoAnteriores.size() != responsaveisRevisaoAtualizado.size()) {
			houveAlteracao = true;
		}else {
			if(tribunaisRevisoresAnteriores == null) {
				tribunaisRevisoresAnteriores = new ArrayList<>();
			}
			if(tribunaisRevisoresAtualizado == null) {
				tribunaisRevisoresAtualizado = new ArrayList<>();
			}
			if(tribunaisRevisoresAnteriores.size() != tribunaisRevisoresAtualizado.size()) {
				houveAlteracao = true;
			}else {
				if(!responsaveisRevisaoAtualizado.containsAll(responsaveisRevisaoAnteriores)) {
					houveAlteracao = true;
				}else {
					if(tribunaisRevisoresAtualizado.containsAll(tribunaisRevisoresAnteriores)) {
						houveAlteracao = true;
					}
				}
			}
		}
		
		return houveAlteracao;
	}
	
	/**
	 * 1. verifica qual a permiss??o do usu??rio:
	 * > admin de labels
	 * -- pode fazer qualquer altera????o
	 * > revisor label X
	 * -- s?? permite que ele indique ou retire a sua label pr??pria label (apenas uma), qualquer outra altera????o deve ser revertida
	 * > sem permiss??es sobre labels
	 * -- qualquer altera????o deve ser revertida
	 */
	private TipoPermissaoMREnum verificaPermissaoUsuario(
				JiraUser revisorJira, GitlabUser revisorGitlab, String tribunalRevisor,
				JiraIssue issue, GitlabMergeRequestAttributes mergeRequest,
				List<String> labelsAdicionadas, List<String> labelsRemovidas) {
		TipoPermissaoMREnum permissaoUsuario = null;
		
		if(revisorJira != null && revisorGitlab != null && mergeRequest != null) {
//			if((jiraService.isServico(revisorJira) || jiraService.isLiderProjeto(revisorJira))) {
//				permissaoUsuario = TipoPermissaoMREnum.COM_PERMISSAO;
//			}else 
			if(!jiraService.isRevisorCodigo(revisorJira)) {
				permissaoUsuario = TipoPermissaoMREnum.SEM_PERMISSAO_NAO_EH_REVISOR;
			}else{
				if(!mergeRequest.getState().equals(GitlabMergeRequestStateEnum.OPENED) || !mergeRequest.getMergeStatus().equals(GitlabMergeRequestStatusEnum.CAN_BE_MERGED)) {
					permissaoUsuario = TipoPermissaoMREnum.SEM_PERMISSAO_MERGE_NAO_MERGEAVEL;
					if(!mergeRequest.getState().equals(GitlabMergeRequestStateEnum.OPENED)) {
						messages.error("O merge n??o est?? aberto");
					}else {
						messages.error("O merge n??o est?? apto a ser mergeado");
					}
				}else {
					String labelAprovacaoUsuario = jiraService.getLabelAprovacaoCodigoDeUsuario(revisorJira);
					Boolean usuarioAlterouSuaLabel = false;
					if(StringUtils.isNotBlank(labelAprovacaoUsuario) && StringUtils.isNotBlank(tribunalRevisor) 
							&& labelAprovacaoUsuario.endsWith(tribunalRevisor)) {
						
						if(labelsAdicionadas != null && labelsAdicionadas.size() == 1
								&& (labelsRemovidas == null || labelsRemovidas.isEmpty())
								&& Utils.compareAsciiIgnoreCase(labelsAdicionadas.get(0), labelAprovacaoUsuario)) {
							usuarioAlterouSuaLabel = true;
						}else if(labelsRemovidas != null && labelsRemovidas.size() == 1
								&& (labelsAdicionadas == null || labelsAdicionadas.isEmpty())
								&& Utils.compareAsciiIgnoreCase(labelsRemovidas.get(0), labelAprovacaoUsuario)) {
							usuarioAlterouSuaLabel = true;
						}
					}else if(StringUtils.isNotBlank(labelAprovacaoUsuario) && StringUtils.isNotBlank(tribunalRevisor)) {
						messages.error("O usu??rio revisor: " + revisorJira.getName() + " est?? configurado para indicar aprova????o: " + labelAprovacaoUsuario 
								+ ", mas pertence ao tribunal: " + tribunalRevisor);
					}
					if(usuarioAlterouSuaLabel) {
						// verifica se o usu??rio n??o ?? o autor do MR, nem ?? o autor do ??ltimo commit, nem ?? o respons??vel pela codifica????o da issue
						GitlabUser autorUltimoCommit = gitlabService.getLastCommitAuthor(mergeRequest);
						if(autorUltimoCommit != null && autorUltimoCommit.getId().equals(revisorGitlab.getId())) {
							permissaoUsuario = TipoPermissaoMREnum.SEM_PERMISSAO_AUTOR_COMMIT;
						}else {
							JiraUser responsavelCodificacao = issue.getFields().getResponsavelCodificacao();
							if(responsavelCodificacao.getKey().equalsIgnoreCase(revisorJira.getKey())) {
								permissaoUsuario = TipoPermissaoMREnum.SEM_PERMISSAO_RESPONSAVEL_CODIFICACAO;
							}
						}
						if(permissaoUsuario == null) {
							permissaoUsuario = TipoPermissaoMREnum.COM_PERMISSAO;
						}
					}else {
						permissaoUsuario = TipoPermissaoMREnum.SEM_PERMISSAO_ALTEROU_LABEL_ERRADA;
					}
				}
			}
		}
		return permissaoUsuario;
	}
	
	private List<String> atualizaListaTribunaisRevisores(boolean adicionaRevisor, List<String> tribunaisRevisores, String tribunalRevisorAtual){
		if(StringUtils.isNotBlank(tribunalRevisorAtual)) {
			if(tribunaisRevisores == null) {
				tribunaisRevisores = new ArrayList<>();
			}
			if(adicionaRevisor) {
				if(!tribunaisRevisores.contains(tribunalRevisorAtual)) {
					tribunaisRevisores.add(tribunalRevisorAtual);
				}
			}else {
				if(tribunaisRevisores.contains(tribunalRevisorAtual)) {
					tribunaisRevisores.remove(tribunalRevisorAtual);
				}
			}
		}
		return tribunaisRevisores;
	}
	
	private List<String> recuperaLabelsAprovacao(List<String> labelsMR){
		List<String> labelsAprovacao = new ArrayList<>();
		if(labelsMR != null) {
			for (String labelMR : labelsMR) {
				if(StringUtils.isNotBlank(labelMR) && (labelMR.startsWith(GitlabService.PREFIXO_LABEL_APROVACAO_TRIBUNAL)) ) {
					String siglaTribunal = labelMR.replaceAll(GitlabService.PREFIXO_LABEL_APROVACAO_TRIBUNAL, "").replaceAll("_", "").trim();
					labelsAprovacao.add(siglaTribunal);
				}
			}
		}
		return labelsAprovacao;
	}
	
	private List<String> identificaLabelsAprovacaoPeloSufixo(List<String> sufixos, List<String> labelsOriginais){
		List<String> labelsAprovacao = new ArrayList<>();
		if(labelsOriginais != null && sufixos != null) {
			for (String sufixo : sufixos) {
				if(StringUtils.isNotBlank(sufixo)) {
					for (String labelOriginal : labelsOriginais) {
						if(StringUtils.isNotBlank(labelOriginal) && labelOriginal.startsWith(GitlabService.PREFIXO_LABEL_APROVACAO_TRIBUNAL) && labelOriginal.endsWith(sufixo)) {
							labelsAprovacao.add(labelOriginal);
							break;
						}
					}
				}
			}
		}
		return labelsAprovacao;
	}
	
	private List<String> identificaLabelsSemTribunalRevisorNaIssue(List<String> labelsAprovacao, List<String> tribunaisRevisores){
		if(labelsAprovacao == null) {
			labelsAprovacao = new ArrayList<>();
		}
		if(tribunaisRevisores == null) {
			tribunaisRevisores = new ArrayList<>();
		}
		List<String> labelsSemTribunaisAprovadores = new ArrayList<>();
		if(!tribunaisRevisores.containsAll(labelsAprovacao)) {
			for (String labelAprovacao : labelsAprovacao) {
				if(!tribunaisRevisores.contains(labelAprovacao)) {
					labelsSemTribunaisAprovadores.add(labelAprovacao);
				}
			}
		}
		return labelsSemTribunaisAprovadores;
	}
	
	private List<String> identificaTribunalRevisorSemLabelAprovacao(List<String> labelsAprovacao, List<String> tribunaisRevisores){
		if(labelsAprovacao == null) {
			labelsAprovacao = new ArrayList<>();
		}
		if(tribunaisRevisores == null) {
			tribunaisRevisores = new ArrayList<>();
		}
		List<String> tribunaisRevisoresSemLabel = new ArrayList<>();
		if(!labelsAprovacao.containsAll(tribunaisRevisores)) {
			for (String tribunal : tribunaisRevisores) {
				if(!labelsAprovacao.contains(tribunal)) {
					tribunaisRevisoresSemLabel.add(tribunal);
				}
			}
		}
		return tribunaisRevisoresSemLabel;
	}
	
	private List<JiraUser> removeUsuariosDosTribunais(List<JiraUser> usuarios, List<String> tribunais){
		List<JiraUser> novaListaUsuarios = new ArrayList<>();
		if(usuarios != null && tribunais != null && !tribunais.isEmpty()) {
			for (JiraUser usuario : usuarios) {
				String tribunalUsuario = jiraService.getTribunalUsuario(usuario, false);
				if(StringUtils.isBlank(tribunalUsuario) || !tribunais.contains(tribunalUsuario)) {
					novaListaUsuarios.add(usuario);
				}
			}
		}else {
			novaListaUsuarios = usuarios;
		}
		return novaListaUsuarios;
	}
	
	private void removeLabelsSemTribunalRevisorNaIssue(GitlabMergeRequestAttributes mergeRequest, JiraIssue issue,
			List<String> labelsAprovacoesTribunais, List<String> tribunaisRevisores, List<String> labelsOriginais) throws Exception {
		
		List<String> labelsSemTribunaisAprovadores = identificaLabelsSemTribunalRevisorNaIssue(labelsAprovacoesTribunais, tribunaisRevisores);

		// retirar estas labels do MR
		if(mergeRequest != null && labelsSemTribunaisAprovadores != null && !labelsSemTribunaisAprovadores.isEmpty()) {
			BigDecimal mrIID = mergeRequest.getIid();
			String gitlabProjectId = mergeRequest.getTargetProjectId().toString();

			List<String> nomesLabelsRemover = identificaLabelsAprovacaoPeloSufixo(labelsSemTribunaisAprovadores, labelsOriginais);
			GitlabMRResponse response = gitlabService.removeLabelsMR(mergeRequest, nomesLabelsRemover);
			if(response == null) {
				messages.error("Houve um erro ao tentar remover as labels do MR :: MRIID=" + mrIID 
						+ " - labels:\n" + nomesLabelsRemover);
			}else {
				StringBuilder mensagemRemocaoLabels = new StringBuilder();
				GitlabMarkdown gitlabMarkdown = new GitlabMarkdown();
				String issueURL = JiraUtils.getPathIssue(issue.getKey(), jiraUrl);
				
				boolean isPlural = (nomesLabelsRemover.size() > 1);
				if(isPlural) {
					mensagemRemocaoLabels
						.append(gitlabMarkdown.normal("As labels: "));
				}else {
					mensagemRemocaoLabels
						.append(gitlabMarkdown.normal("A label: "));
				}
				mensagemRemocaoLabels
					.append(gitlabMarkdown.normal(String.join(", ", nomesLabelsRemover)));
				if(isPlural) {
					mensagemRemocaoLabels
						.append(gitlabMarkdown.normal(" foram removidas "));
				}else {
					mensagemRemocaoLabels
						.append(gitlabMarkdown.normal(" foi removida "));
				}
				mensagemRemocaoLabels
					.append(gitlabMarkdown.normal(" por n??o haver correspond??ncia na issue "))
					.append(gitlabMarkdown.link(issueURL, issue.getKey()));
				gitlabService.sendMergeRequestComment(gitlabProjectId, mrIID, mensagemRemocaoLabels.toString());
			}
		}
	}
}