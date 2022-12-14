package dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.ProcessingMessage;
import com.devplatform.model.bot.VersionReleaseNotes;
import com.devplatform.model.gitlab.GitlabTag;
import com.devplatform.model.gitlab.GitlabTagRelease;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.JiraIssueTipoVersaoEnum;
import com.devplatform.model.jira.JiraIssuetype;
import com.devplatform.model.jira.JiraUser;
import com.devplatform.model.jira.JiraVersion;
import com.devplatform.model.jira.event.JiraEventIssue;

import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.JiraUtils;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.AsciiDocMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.GitlabMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.JiraMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.RocketchatMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.SlackMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.TelegramMarkdownHtml;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.NewVersionReleasedNewsTextModel;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.NewVersionReleasedSimpleCallTextModel;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.ReleaseNotesTextModel;

@Component
public class LanVersion060FinishReleaseNotesProcessingHandler extends Handler<JiraEventIssue>{

	private static final Logger logger = LoggerFactory.getLogger(LanVersion060FinishReleaseNotesProcessingHandler.class);

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "|VERSION-LAUNCH||060||PUBLISH-DOCS|";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Value("${project.documentation.url}")
	private String DOCSURL;

	@Autowired
	private ReleaseNotesTextModel releaseNotesModel;

	@Autowired
	private NewVersionReleasedNewsTextModel newVersionReleasedNewsModel;

	@Autowired
	private NewVersionReleasedSimpleCallTextModel newVersionSimpleCallModel;

	/**
	 * :: Finalizar versao ::
	 *    Tenta recuperar o release notes do arquivo anexado ?? issue
	 *    Atualiza a informa????o de autor e data da release
	 *    
	 *    Gerar release notes no projeto docs.pje.jus.br
	 *    Comunicar lan??amento da vers??o
	 *    Criar a issue para o lan??amento da pr??xima vers??o
	 *    Lan??ar evento no rabbit
	 */
	public void handle(JiraEventIssue jiraEventIssue) throws Exception {
	}
	
	public void processReleaseNotes(VersionReleaseNotes releaseNotes) throws Exception {
		messages.clean();
//		if (jiraEventIssue != null && jiraEventIssue.getIssue() != null) {
//			JiraIssue issue = jiraEventIssue.getIssue();
			// 1. verifica se ?? do tipo "geracao de nova versao"
//			if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_NEW_VERSION) &&
//					JiraUtils.isIssueInStatus(issue, JiraService.STATUS_RELEASE_NOTES_FINISHPROCESSING_ID) &&
//					JiraUtils.isIssueChangingToStatus(jiraEventIssue, JiraService.STATUS_RELEASE_NOTES_FINISHPROCESSING_ID)) {

//				messages.setId(issue.getKey());
//				messages.debug(jiraEventIssue.getIssueEventTypeName().name());

//				VersionReleaseNotes releaseNotes = null;
				String dataReleaseNotesStr = Utils.dateToStringPattern(new Date(), JiraService.JIRA_DATETIME_PATTERN);

				// 4.- a pessoa que solicitou a operacao est?? dentro do grupo de pessoas que podem abrir?
//				if(jiraService.isLancadorVersao(jiraEventIssue.getUser())) {
					// recupera o anexo cocm o json do release notes
//					byte[] file = jiraService.getAttachmentContent(issue,
//							JiraService.RELEASE_NOTES_JSON_FILENAME);
//					String releaseNotesAttachment = new String(file);
//					boolean releaseNotesEncontrado = false;
//					if (releaseNotesAttachment != null) {
//						releaseNotes = Utils.convertJsonToJiraReleaseNotes(releaseNotesAttachment);
//						if (releaseNotes != null && StringUtils.isNotBlank(releaseNotes.getProject())) {
//							releaseNotesEncontrado = true;
//						}
//					}
//					boolean releaseLancada = false;
//					if(releaseNotesEncontrado) {
//						if (StringUtils.isBlank(releaseNotes.getReleaseDate())) {
//							// verifica se a tag da vers??o j?? existe
//							boolean tagJaExistente = false;
//							if (StringUtils.isNotBlank(releaseNotes.getGitlabProjectId())) {
//								GitlabTag tag = gitlabService.getVersionTag(releaseNotes.getGitlabProjectId(),
//										releaseNotes.getVersion());
//								if (tag != null && tag.getCommit() != null) {
//									tagJaExistente = true;
//									releaseNotes.setReleaseDate(tag.getCommit().getCommittedDate().toString());
//									JiraUser usuarioCommiter = null;
//									if (tag.getCommit().getCommitterName() != null) {
//										usuarioCommiter = jiraService
//												.findUserByUserName(tag.getCommit().getCommitterEmail());
//									}
//									if (usuarioCommiter != null) {
//										releaseNotes.setAuthor(usuarioCommiter);
//									}
//								}
//							}
//							if (!tagJaExistente) {
//								messages.error("A tag: " + releaseNotes.getVersion()
//									+ " ainda N??O foi lan??ada.");
//							} else {
//								releaseLancada = true;
//							}
//						}else {
//							messages.info("Esta vers??o foi lan??ada em: " + releaseNotes.getReleaseDate());
//							releaseLancada = true;
//						}
//						if(releaseLancada) {
							// criar a issue de release-notes no projeto de documentacao
//							JiraIssue issueDocumentacao = criarIssueDocumentacaoReleaseNotes(releaseNotes);
//							if(issueDocumentacao != null) {
//								Map<String, Object> updateFields = new HashMap<>();
//								// add link issue anterior
//								jiraService.criarNovoLink(issue, issueDocumentacao.getKey(), 
//										JiraService.ISSUELINKTYPE_DOCUMENTATION_ID.toString(), JiraService.ISSUELINKTYPE_DOCUMENTATION_OUTWARDNAME, false, updateFields);
//								if(updateFields != null && !updateFields.isEmpty()) {
//									enviarAlteracaoJira(issue, updateFields, null, null, false, false);
//								}
//							}

							// Gerar documento asciidoc e incluir no docs.pje.jus.br
//							String versaoAfetada = releaseNotes.getAffectedVersion();
//							releaseNotes.setAffectedVersion(releaseNotes.getVersion());
							// lan??ar o texto do release do gitlab se n??o houver texto na release
//							if(!isTagReleaseCreated(releaseNotes.getGitlabProjectId(), releaseNotes.getVersion())) {
//								GitlabMarkdown gitlabMarkdown = new GitlabMarkdown();
//								releaseNotesModel.setReleaseNotes(releaseNotes);
//								String releaseText = releaseNotesModel.convert(gitlabMarkdown);
//								GitlabTagRelease tagReleaseResponse = gitlabService.createTagRelease(releaseNotes.getGitlabProjectId(), releaseNotes.getVersion(), releaseText);
//								if(tagReleaseResponse != null) {
//									messages.info("Criado o documento de release da tag do projeto: " + releaseNotes.getProject());
//								}else {
//									messages.error("Erro ao criar o documento de release da tag do projeto: " + releaseNotes.getProject());
//								}
//							}
							if(!messages.hasSomeError()) {
//								// comunicar o lan??amento da vers??o
//								Boolean comunicarLancamentoVersaoCanaisOficiais = false;
//								if(issue.getFields().getComunicarLancamentoVersao() != null 
//										&& !issue.getFields().getComunicarLancamentoVersao().isEmpty()) {
//									
//									if(issue.getFields().getComunicarLancamentoVersao().get(0).getValue().equalsIgnoreCase("Sim")) {
//										comunicarLancamentoVersaoCanaisOficiais = true;
//									}
//								}
//								comunicarLancamentoVersao(issue, releaseNotes, comunicarLancamentoVersaoCanaisOficiais);
//								comunicarLancamentoVersao(releaseNotes, true);
								
								// indica a data de lan??amento da vers??o atual no jira, atualizando o n??mero da vers??o tamb??m se necess??rio
//								jiraService.releaseVersionInRelatedProjects(
//											issue.getFields().getProject().getKey(), 
//											versaoAfetada, 
//											releaseNotes.getVersion(), 
//											dataReleaseNotesStr, "Altera????es da vers??o: " + releaseNotes.getVersion());
								
								// verifia se foi solicitada a inicializacao da pr??xima vers??o indicada
//								Boolean iniciarProximaVersaoAutomaticamente = false;
//								if(issue.getFields().getIniciarProximaVersaoAutomaticamente() != null 
//										&& !issue.getFields().getIniciarProximaVersaoAutomaticamente().isEmpty()) {
//									
//									if(issue.getFields().getIniciarProximaVersaoAutomaticamente().get(0).getValue().equalsIgnoreCase("Sim")) {
//										iniciarProximaVersaoAutomaticamente = true;
//										messages.info("Iniciando a issue da pr??xima vers??o: " + releaseNotes.getNextVersion() + " automaticamente como solicitado.");
//									}
//								}
//								if(iniciarProximaVersaoAutomaticamente) {
//									// cria a nova versao nos projetos associados no jira
//									String projectKey = issue.getFields().getProject().getKey();
//									jiraService.createVersionInRelatedProjects(issue.getFields().getProject().getKey(), releaseNotes.getNextVersion());
//									
//									// criar nova issue de lancamento de vers??o para a pr??xima vers??o
//									JiraIssuetype issueType = issue.getFields().getIssuetype();
//									JiraIssue issueProximaVersao = criarIssueLancamentoVersao(projectKey, issueType, releaseNotes);
//									if(issueProximaVersao != null) {
//										// add link issue anterior
//										Map<String, Object> updateFields = new HashMap<>();
//										
//										jiraService.criarNovoLink(issue, issueProximaVersao.getKey(), 
//												JiraService.ISSUELINKTYPE_RELATES_ID.toString(), JiraService.ISSUELINKTYPE_RELATES_OUTWARDNAME, false, updateFields);
//										
//										if(updateFields != null && !updateFields.isEmpty()) {
//											enviarAlteracaoJira(issue, updateFields, null, null, false, false);
//										}
//									}
//								}
							}
//						}else {
//							messages.error("A release ainda n??o foi lan??ada.");
//						}
//					}else {
//						messages.error("Release notes n??o encontrado na issue de refer??ncia: " + issue.getKey());
//					}
//				}else {
//					messages.error("O usu??rio: [~" + jiraEventIssue.getUser().getName() + "] - n??o tem permiss??o para fazer esta opera????o.");
//				}
//				Map<String, Object> updateFields = new HashMap<>();
//				if(messages.hasSomeError()) {
//					// tramita para o impedmento, enviando as mensagens nos coment??rios
//					jiraService.adicionarComentario(issue, messages.getMessagesToJira(), updateFields);
//					enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_IMPEDIMENTO, true, true);
//				}else {
//					// tramita automaticamente, enviando as mensagens nos coment??rios
//					// atualiza o anexo com o backup do relase em json
//					String releaseNotesJson = Utils.convertObjectToJson(releaseNotes);
//					jiraService.sendTextAsAttachment(issue, JiraService.RELEASE_NOTES_JSON_FILENAME, releaseNotesJson);
//
//					// atualiza a descricao da issue com o novo texto
//					JiraMarkdown jiraMarkdown = new JiraMarkdown();
//					releaseNotesModel.setReleaseNotes(releaseNotes);
//					String jiraMarkdownRelease = releaseNotesModel.convert(jiraMarkdown);
//					jiraService.atualizarDescricao(issue, jiraMarkdownRelease, updateFields);
//
//					// atualiza a data de lancamento do release notes
//					jiraService.atualizarDataReleaseNotes(issue, dataReleaseNotesStr, updateFields);
//					// atualiza link para o release notes na issue
//					jiraService.atualizarURLPublicacao(issue, releaseNotes.getUrl(), updateFields);
//					
//					jiraService.adicionarComentario(issue, messages.getMessagesToJira(), updateFields);
//					enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_FINALIZAR_DEMANDA, true, true);
//				}
//			}
//		}
	}
	
	public List<ProcessingMessage> comunicateOfficialLaunch(VersionReleaseNotes releaseNotes, Boolean test) {
		messages.clean();
		
		String mensagemRoketchat = gerarMensagemRocketchat(releaseNotes);
		if(StringUtils.isNotBlank(mensagemRoketchat)) {
			rocketchatService.sendBotMessage(mensagemRoketchat);
			if (BooleanUtils.isFalse(test)) {
				rocketchatService.sendMessageGeral(mensagemRoketchat, false);
			}
				
			messages.info("Mensagem enviada ao rocket.chat");

		}

		String mensagemTelegram = gerarMensagemTelegram(releaseNotes);
		if(StringUtils.isNotBlank(mensagemTelegram)) {
			telegramService.sendBotMessageHtml(mensagemTelegram);
			if (BooleanUtils.isFalse(test)) {
				telegramService.sendMessagePJeNewsHtml(mensagemTelegram);
			}
			messages.info("Mensagem enviada ao telegram");

		}

//		liberaVersaoJira(releaseNotes.getIssueKey(), releaseNotes.getVersion());
	
		return messages.messages;
	}

	private void liberaVersaoJira(String jiraProjectKey, String appVersion) {
		List<String> relatedProjectKeys = jiraService.getJiraRelatedProjects(jiraProjectKey);
		for (String relatedProjectKey : relatedProjectKeys) {
			JiraVersion jiraVersion = jiraService.findProjecVersion(relatedProjectKey, appVersion);
			if (jiraVersion != null) {
				jiraVersion.setReleaseDate(Utils.dateToStringPattern(Calendar.getInstance().getTime(), Utils.DATE_PATTERN_SIMPLE));
				jiraVersion.setReleased(true);
			
				jiraService.updateProjectVersion(jiraVersion);
				messages.info("Vers??o " + appVersion + " do projeto " + relatedProjectKey + " finalizada em " + Utils.dateToStringPattern(Calendar.getInstance().getTime(), Utils.DATE_PATTERN_SIMPLE));
			} else {
				messages.info("Vers??o " + appVersion + " n??o encontrada no projeto " + relatedProjectKey);
			}
		}
	}
	
	private String gerarMensagemTelegram(VersionReleaseNotes releaseNotes) {
		newVersionReleasedNewsModel.setReleaseNotes(releaseNotes);
		return newVersionReleasedNewsModel.convert(new TelegramMarkdownHtml());
	}

	private String gerarMensagemRocketchat(VersionReleaseNotes releaseNotes) {
		newVersionSimpleCallModel.setReleaseNotes(releaseNotes);
		return newVersionSimpleCallModel.convert(new RocketchatMarkdown());
	}

	
	/**
	 * Cria uma nova issue:
	 * 	- tipo: lancamento de uma nova versao
	 *  - n??mero da versao sendo o n??mero da versao indicada como pr??xima da issue atual
	 *  - tipo de versao: ordinaria
	 *  - se o projeto implementa gitflow, entao ter?? a marcacao de criacao autom??tica da release candidate
	 *  
	 * @param projectKey
	 * @param issueType
	 * @param releaseNotes
	 * @throws Exception
	 */
	private JiraIssue criarIssueLancamentoVersao(String projectKey, JiraIssuetype issueType, VersionReleaseNotes releaseNotes) throws Exception {
		JiraIssue issueRelacionada = null;
		// verifica se j?? existe uma issue com estas caracter??sticas
		String jql = jiraService.getJqlIssuesLancamentoVersao(releaseNotes.getNextVersion(), projectKey, false);
		
		List<JiraIssue> issues = jiraService.getIssuesFromJql(jql);
		if(issues == null || issues.isEmpty()) {
			Map<String, Object> issueFields = new HashMap<>();
			Map<String, Object> issueUpdateFields = new HashMap<>();

			// add project
			jiraService.novaIssueCriarProject(projectKey, issueFields);
			// add issueType
			jiraService.novaIssueCriarIssueType(issueType, issueFields);
			// add summary
			jiraService.novaIssueCriarSummary("Lan??amento da vers??o " + releaseNotes.getNextVersion(), issueFields);
			jiraService.novaIssueCriarIndicacaoPrepararVersaoAutomaticamente(issueFields);
			// add versao afetada (version)
			JiraVersion versao = new JiraVersion();
			versao.setName(releaseNotes.getNextVersion());
			jiraService.novaIssueCriarAffectedVersion(versao, issueFields);
			// add tipo de versao (ordinaria)
			jiraService.novaIssueCriarTipoVersao(JiraIssueTipoVersaoEnum.ORDINARIA, issueFields);
			// add link issue anterior
			jiraService.criarNovoLink(null, releaseNotes.getIssueKey(), 
					JiraService.ISSUELINKTYPE_RELATES_ID.toString(), JiraService.ISSUELINKTYPE_RELATES_INWARDNAME, true, issueUpdateFields);

			issueRelacionada = enviarCriacaoJiraIssue(issueFields, issueUpdateFields);
			
		}else {
			issueRelacionada = issues.get(0);
			messages.info("A issue da pr??xima vesrs??o j?? existe: " + issueRelacionada.getKey());
		}
		return issueRelacionada;
	}
	
	private JiraIssue criarIssueDocumentacaoReleaseNotes(VersionReleaseNotes releaseNotes) throws Exception {
		JiraIssue relatedIssueDoc = null;
		String projectKey = JiraService.PROJECTKEY_PJEDOC;
		String issueTypeId = JiraService.ISSUE_TYPE_RELEASE_NOTES.toString();
		String versaoASerLancada = releaseNotes.getVersion();
		String issueProjectKey = JiraUtils.getProjectKeyFromIssueKey(releaseNotes.getIssueKey());
		String summary = "[" + issueProjectKey + "] Release notes do projeto: " + releaseNotes.getProject() + " - versao: " + versaoASerLancada;
		
		// gera o conte??do da release em asciidoc
		releaseNotesModel.setReleaseNotes(releaseNotes);
		String textoAnexo = releaseNotesModel.convert(new AsciiDocMarkdown());
		StringBuilder nomeArquivoAnexoSB = new StringBuilder(JiraService.RELEASE_NOTES_FILENAME_PREFIX)
				.append(versaoASerLancada.replaceAll("\\.", "-")).append(JiraService.FILENAME_SUFFIX_ADOC);
		
//		releaseNotesModel.setReleaseNotes(releaseNotes);
		String description = releaseNotesModel.convert(new JiraMarkdown());

		// 0. identificar o projeto de documentacao + tipo de issue de relase notes
		
		// 1. verificar se a issue j?? n??o existe
		// 2. montar campos da nova issue:
		// - projeto
		// - tipo
		// - estrutura
		// - versao a ser lancada
		// - resumo
		// - descricao
		// - publicar doc automaticamente? = SIM

		// Identificacao da estrutura de documentacao
		String identificacaoEstruturaDocumentacao = jiraService.getEstruturaDocumentacao(issueProjectKey);
		String opcaoId = null;
		if(StringUtils.isNotBlank(identificacaoEstruturaDocumentacao)) {
			String[] opcoes = identificacaoEstruturaDocumentacao.split(":");
			if(opcoes != null && opcoes.length > 0) {
				opcaoId = opcoes[(opcoes.length - 1)];
			}
		}
		
		if(StringUtils.isNotBlank(opcaoId)) {
			// verifica se j?? existe uma issue com estas caracter??sticas
			String jql = jiraService.getJqlIssuesDocumentacaoReleaseNotes(versaoASerLancada, opcaoId);
			
			List<JiraIssue> issues = jiraService.getIssuesFromJql(jql);
			if(issues == null || issues.isEmpty()) {
				Map<String, Object> issueFields = new HashMap<>();
				Map<String, Object> issueUpdateFields = new HashMap<>();
				// add project
				jiraService.novaIssueCriarProject(projectKey, issueFields);
				// add issueType
				jiraService.novaIssueCriarIssueTypeId(issueTypeId, issueFields);
				// add summary
				jiraService.novaIssueCriarSummary(summary, issueFields);
				// add description
				jiraService.novaIssueCriarDescription(description, issueFields);
				
				// add Publicar documentacao automaticamente? = sim
				jiraService.novaIssueCriarIndicacaoPublicarDocumentacaoAutomaticamente(issueFields);
				// add versao a ser lancada
				jiraService.novaIssueCriarVersaoASerLancada(versaoASerLancada, issueFields);
				// criar j?? na cria????o da issue um link para a issue de lancamento de versao
				jiraService.criarNovoLink(null, releaseNotes.getIssueKey(), 
						JiraService.ISSUELINKTYPE_DOCUMENTATION_ID.toString(), JiraService.ISSUELINKTYPE_DOCUMENTATION_INWARDNAME, true, issueUpdateFields);
				
				// identificar estrutura da documentacao
				// add estrutura documentacao
				jiraService.novaIssueCriarEstruturaDocumentacao(identificacaoEstruturaDocumentacao, issueFields);
				relatedIssueDoc = enviarCriacaoJiraIssue(issueFields, issueUpdateFields);
				
				if(relatedIssueDoc != null) {
					// envia o conte??do do texto como asciidoc na issue remota
					jiraService.sendTextAsAttachment(relatedIssueDoc, nomeArquivoAnexoSB.toString(), textoAnexo);
				}
			}else {
				relatedIssueDoc = issues.get(0);
				messages.info("A issue da documenta????o desta vesrs??o j?? existe: " + relatedIssueDoc.getKey());
			}
		}else {
			messages.error("N??o foi poss??vel identificar automaticamente qual ?? a estrutura de "
					+ "documenta????o relacionada para este projeto " + issueProjectKey 
					+ " no projeto de documenta????o " + projectKey);
		}
				
		return relatedIssueDoc;
	}
	
	private boolean isTagReleaseCreated(String gitlabProjectId, String tagName) {
		boolean tagReleaseCreated = false;

		GitlabTag tag = gitlabService.getVersionTag(gitlabProjectId, tagName);
		if (tag != null && tag.getRelease() != null) {
			tagReleaseCreated = StringUtils.isNotBlank(tag.getRelease().getDescription());
		}
		
		return tagReleaseCreated;
	}
}