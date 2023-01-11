package dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.ProcessingMessage;
import com.devplatform.model.bot.VersionReleaseNotes;
import com.devplatform.model.gitlab.GitlabTag;
import com.devplatform.model.gitlab.GitlabTagRelease;
import com.devplatform.model.gitlab.response.GitlabBranchResponse;
import com.devplatform.model.gitlab.response.GitlabCommitResponse;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.JiraUser;
import com.devplatform.model.jira.event.JiraEventIssue;

import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.GitlabService;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.JiraUtils;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.GitlabMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.JiraMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.ReleaseNotesTextModel;

@Component
public class LanVersion050ProcessReleaseNotesHandler extends Handler<JiraEventIssue>{

	private static final Logger logger = LoggerFactory.getLogger(LanVersion050ProcessReleaseNotesHandler.class);

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "|VERSION-LAUNCH||050||PROCESS-RELEASE-NOTES|";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Autowired
	private ReleaseNotesTextModel releaseNotesModel;

	/**
	 * :: Processando release notes ::
	 *    Verifica se há o backup do release notes como anexo da issue
	 *    E o release notes encontrado ainda não tenha sido processado (data de relase = null)
	 *    Verificar se a tag já foi lançada, se não:
	 *    	Atualizar nome da versão no pom
	 *   	Validar nome da pasta de scripts da versão
	 *    	Integrar branch release na master
	 *    	Gerar tag da versão
	 */
	public void handle(JiraEventIssue jiraEventIssue) throws Exception {
	}
	
	public void finalizaVersaoGitlab(VersionReleaseNotes releaseNotes) {
		messages.clean();
//		if (jiraEventIssue != null && jiraEventIssue.getIssue() != null) {
//			JiraIssue issue = jiraEventIssue.getIssue();
			// 1. verifica se é do tipo "geracao de nova versao"
//			if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_NEW_VERSION) &&
//					JiraUtils.isIssueInStatus(issue, JiraService.STATUS_RELEASE_NOTES_CONFIRMED_ID) &&
//					JiraUtils.isIssueChangingToStatus(jiraEventIssue, JiraService.STATUS_RELEASE_NOTES_CONFIRMED_ID)) {

//				messages.setId(issue.getKey());
//				messages.debug(jiraEventIssue.getIssueEventTypeName().name());

				// 4.- a pessoa que solicitou a operacao está dentro do grupo de pessoas que podem abrir?
//				VersionReleaseNotes releaseNotes = null;
//				if (jiraService.isLancadorVersao(jiraEventIssue.getUser())) {
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
//					if (releaseNotesEncontrado) {
//						if (StringUtils.isBlank(releaseNotes.getReleaseDate())) {
//							// 1. identifica se o projeto utiliza ou não o gitflow - branch principal do
//							// projeto
							Boolean implementsGitflow = false;
							if (StringUtils.isNotBlank(releaseNotes.getGitlabProjectId())) {
								implementsGitflow = gitlabService
										.isProjectImplementsGitflow(releaseNotes.getGitlabProjectId());
							}
//							// verifica se a tag da versão já existe
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
								finalizaVersaoGitlab(implementsGitflow, releaseNotes);
//							} else {
//								messages.info("A tag: " + releaseNotes.getVersion()
//										+ " já foi lançada, não é possível regerá-la.");
//							}
//						}else {
//							messages.info("Parece que esta versão já foi lançada em: " + releaseNotes.getReleaseDate());
//						}
//					} else {
//						messages.error("Release notes não encontrado como anexo da issue.");
//					}
//				} else {
//					messages.error("O usuário [~" + jiraEventIssue.getUser().getKey()
//							+ "] não possui permissão para lançar a versão.");
//				}
//				if(messages.hasSomeError()) {
//					// tramita para o impedmento, enviando as mensagens nos comentários
//					Map<String, Object> updateFieldsErrors = new HashMap<>();
//					jiraService.adicionarComentario(issue, messages.getMessagesToJira(), updateFieldsErrors);
//					enviarAlteracaoJira(issue, updateFieldsErrors, null, JiraService.TRANSITION_PROPERTY_KEY_IMPEDIMENTO, true, true);
//				}else {
//					// tramita automaticamente, enviando as mensagens nos comentários
//					Map<String, Object> updateFields = new HashMap<>();
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
//					// atualiza a data de lancamento da tag
//					String dataTagStr = null;
//					if(releaseNotes.getReleaseDate() != null) {
//						Date dataReleaseNotes = Utils.getDateFromString(releaseNotes.getReleaseDate());
//						dataTagStr = Utils.dateToStringPattern(dataReleaseNotes, JiraService.JIRA_DATETIME_PATTERN);
//					}
//					jiraService.atualizarDataGeracaoTag(issue, dataTagStr, updateFields);
//
//					jiraService.adicionarComentario(issue, messages.getMessagesToJira(), updateFields);
//					enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_SAIDA_PADRAO, true, true);
//				}
//			}
//		}
	}
	
	public List<ProcessingMessage> createReleaseBranch(String gitlabProjectId, String newVersion) {
		messages.clean();
		
		String releaseBranchName = GitlabService.BRANCH_RELEASE_CANDIDATE_PREFIX + newVersion;
		GitlabBranchResponse gitlabBranchResponse = gitlabService.getSingleRepositoryBranch(gitlabProjectId, releaseBranchName);
		
		if (gitlabBranchResponse == null) {
			String branchRef = isImplementsGitflow(gitlabProjectId) ? GitlabService.BRANCH_DEVELOP : GitlabService.BRANCH_MASTER;
			gitlabBranchResponse = gitlabService.createBranch(gitlabProjectId, releaseBranchName, branchRef);
			
			messages.info("Branch " + releaseBranchName + " criado a partir do branch " + branchRef + ". Acesse este branch em " + gitlabBranchResponse.getWebUrl());
		} else {
			messages.info("Branch " + releaseBranchName + " já existe. Acesse este branch em " + gitlabBranchResponse.getWebUrl());
		}
		
		return messages.messages;
	}
	
	public List<ProcessingMessage> updateDevelepmentBranch(String gitlabProjectId, String newVersion) {
		messages.clean();
		
		String developmentBranchName = isImplementsGitflow(gitlabProjectId) ? GitlabService.BRANCH_DEVELOP : GitlabService.BRANCH_MASTER;
		String actualVersion = gitlabService.getVersion(gitlabProjectId, developmentBranchName, false);
		if (StringUtils.isNotBlank(actualVersion)) {
			if(actualVersion.contains(newVersion)) {
				messages.info("Número da versão do sistema consta como atualizado no branch " + developmentBranchName);
			} else {
				String commitMessage = "[RELEASE] Início da versão " + newVersion;
				try {
					GitlabCommitResponse gitlabCommitResponse = gitlabService.atualizaBranchNovaVersao(gitlabProjectId, developmentBranchName, commitMessage, actualVersion, newVersion);
					
					messages.info("Branch " + developmentBranchName + " atualizado para um novo ciclo de desenvolvimento");
				} catch (Exception e) {
					messages.info(e.getLocalizedMessage());
				}
			}
		} else {
			messages.info("Não foi possível identificar o número da versão do sistema para o branch " + developmentBranchName);
		}
		
		return messages.messages;
	}
	
	private boolean isImplementsGitflow(String gitlabProjectId) {
		Boolean implementsGitflow = false;
		if (StringUtils.isNotBlank(gitlabProjectId)) {
			implementsGitflow = this.gitlabService.isProjectImplementsGitflow(gitlabProjectId);
		}
		return implementsGitflow;
	}
	
	private MessagesLogger atualizaPastaScripts(String gitlabProjectId, String newVersion) {
		messages.clean();
		
//		String branchName = GitlabService.BRANCH_MASTER;
//		if (this.isImplementsGitflow(gitlabProjectId)) {
		String branchName = GitlabService.BRANCH_RELEASE_CANDIDATE_PREFIX + newVersion;
//		}
		
		// verificar se o branch existe
		GitlabBranchResponse branch = gitlabService.getSingleRepositoryBranch(gitlabProjectId, branchName);
		if(branch != null) {
			String actualVersion = gitlabService.getVersion(gitlabProjectId, branchName, false);
//			String lastCommitId = null;
			
			// verifica se é necessário alterar o POM
//			if(StringUtils.isNotBlank(actualVersion) && StringUtils.isNotBlank(launchVersion)) {
//				if(!actualVersion.equalsIgnoreCase(launchVersion)) {
//					String commitMessage = "[RELEASE] Atualização do número da versão no arquivo pom.xml para " + launchVersion;
//					GitlabCommitResponse pomResponse = gitlabService.atualizaVersaoPom(gitlabProjectId, branchName, launchVersion, actualVersion, commitMessage);
//					if(pomResponse != null) {
//						messages.info("Atualizada a versão do POM.XML de: " + actualVersion + " - para: " + launchVersion);
//						lastCommitId = pomResponse.getId();
						
						gitlabService.atualizaNumeracaoPastaScriptsVersao(gitlabProjectId, branchName, newVersion, actualVersion, "[RELEASE] Renomeando pasta/arquivos de scripts SQL");
						if(!Utils.clearVersionNumber(actualVersion).equalsIgnoreCase(newVersion)) {
							messages.info("Renomeado pasta/arquivos de scripts SQL de: " + actualVersion + " - para: " + newVersion);
						} else {
							messages.info("Não há necessidade de renomear pasta/arquivos de scripts SQL. A versão atual " + actualVersion + " é igual a versão a ser lançada " + newVersion);
						}
//					}else {
//						messages.error("Falhou ao tentar atualizar o POM.XML de: " + actualVersion + " - para: " + launchVersion);
//					}
//				}else {
//					messages.info("O POM.XML já está atualizado");
//				}
//			}else {
//				messages.error("Não foi possível identificar a versão anterior (" + actualVersion + ") ou a versão que será lançada: (" + launchVersion + ")");
//			}
		}else {
			messages.error(String.format("Branch %s não encontrado no projeto %s", branchName, gitlabProjectId));
		}
		return this.messages;
	}
	
	public List<ProcessingMessage> integraReleaseBranchNoBranchMaster(String gitlabProjectId, String launchVersion) {
		messages.clean();

		String branchName = this.isImplementsGitflow(gitlabProjectId) ? GitlabService.BRANCH_RELEASE_CANDIDATE_PREFIX + launchVersion : GitlabService.BRANCH_MASTER;

		if (StringUtils.isNotBlank(branchName)) {
			this.atualizaPastaScripts(gitlabProjectId, launchVersion);

			String commitMessage = "[RELEASE] Integração do branch " + branchName;

			GitlabMRResponse mrAccepted = null;
			try {
				mrAccepted = gitlabService.mergeBranchReleaseIntoMaster(gitlabProjectId, branchName, commitMessage);
			} catch (GitlabException ex) {
				messages.error(ex.getLocalizedMessage());
			}
			if (mrAccepted == null) {
				messages.error("Falha ao integrar o source branch " + branchName + " ao target branch " + GitlabService.BRANCH_MASTER + ". Verifique o log da aplicação para mais detalhes.");
			} else {
				messages.info("Source branch " + branchName + " integrado ao target branch " + GitlabService.BRANCH_MASTER);
			}
		} else {
			messages.error("Branch " + branchName + " não encontrado para o projeto " + gitlabProjectId);
		}
		return messages.messages;
	}
	
	public List<ProcessingMessage> criaTag(String newVersion, String gitlabProjectId) {
		messages.clean();

		String launchMessage = "[RELEASE] Lançamento da versão " + newVersion;

		String branchName = GitlabService.BRANCH_MASTER;
		String actualVersion = this.gitlabService.getVersion(gitlabProjectId, branchName, false);
		if (StringUtils.isNotBlank(actualVersion) && StringUtils.isNotBlank(newVersion)) {
			if (!actualVersion.equalsIgnoreCase(newVersion)) {
				GitlabCommitResponse pomResponse = this.gitlabService.atualizaVersaoPom(gitlabProjectId, branchName, newVersion, actualVersion, launchMessage);
				if (pomResponse == null) {
					messages.error("Arquivo não alterado. Verifique nos logs o motivo");
				} else {
					messages.error("Arquivo alterado contendo a nova versão (" + newVersion + ")");
				}
			} else {
				messages.info("O arquivo POM.XML já está atualizado com a versão a ser lançada (" + actualVersion + ")");
			}
		} else {
			messages.error("Não foi possível identificar a versão anterior (" + actualVersion + ") ou a versão que será lançada (" + newVersion + ")");
		}
		
		// ==========================

		this.gitlabService.createVersionTag(gitlabProjectId, newVersion, branchName, launchMessage);
		messages.info("Tag " + newVersion + " criada");

		return messages.messages;
	}

	/**
	 * Finaliza a versao:
	 * - se estiver usando o gitflow:
	 * -- atualiza o POM do branch release para a nova versao
	 * -- atualiza número da pasta de scripts/+scripts se for necessário ainda no branch release
	 * -- faz o merge do branch release com o branch master
	 * - se NAO estiver usando o gitflow:
	 * -- atualiza o POM do branch master
	 * -- atualiza número da pasta de scripts/+scripts se for necessario, no branch master
	 * - gera a TAG da versao a partir do branch master
	 * @param implementsGitflow
	 * @param releaseNotes
	 */
	public void finalizaVersaoGitlab(Boolean implementsGitflow, VersionReleaseNotes releaseNotes) {		
		String gitlabProjectId = releaseNotes.getGitlabProjectId();

		String branchName = GitlabService.BRANCH_MASTER;
		if (implementsGitflow) {
//			branchName = GitlabService.BRANCH_RELEASE_CANDIDATE_PREFIX + releaseNotes.getAffectedVersion();
			branchName = GitlabService.BRANCH_RELEASE_CANDIDATE_PREFIX + releaseNotes.getVersion();
//			messages.info("Projeto " + gitlabProjectId + " do gitlab utiliza gitflow");
		}
		// verificar se o branch existe
		GitlabBranchResponse repositoryBranch = gitlabService.getSingleRepositoryBranch(gitlabProjectId, branchName);
		if(repositoryBranch != null) {
			messages.info("Fazendo as alterações no branch: "+ branchName + " do projeto: " + gitlabProjectId);
			String actualVersion = gitlabService.getVersion(gitlabProjectId, branchName, false);
			String lastCommitId = null;
			String newVersion = releaseNotes.getVersion();
			// verifica se é necessário alterar o POM
			if(StringUtils.isNotBlank(actualVersion) && StringUtils.isNotBlank(newVersion)) {
				if(!actualVersion.equalsIgnoreCase(newVersion)) {
//					String commitMessage = "[" + releaseNotes.getIssueKey() + "] Atualiza numero da versao no POM.XML " + releaseNotes.getVersion();
					String commitMessage = "[RELEASE] Atualiza numero da versao no POM.XML " + releaseNotes.getVersion();
					GitlabCommitResponse pomResponse = gitlabService.atualizaVersaoPom(gitlabProjectId, branchName, newVersion, actualVersion, commitMessage);
					if(pomResponse != null) {
						messages.info("Atualizada a versão do POM.XML de: " + actualVersion + " - para: " + newVersion);
						lastCommitId = pomResponse.getId();
					}else {
						messages.error("Falhou ao tentar atualizar o POM.XML de: " + actualVersion + " - para: " + newVersion);
					}
				}else {
					messages.info("O POM.XML já está atualizado");
				}
			}else {
				messages.error("Não foi possível identificar a versão anterior (" + actualVersion + ") ou a versão que será lançada: (" + newVersion + ")");
			}
			if(!messages.hasSomeError()) {
//				String commitMessage = "[" + releaseNotes.getIssueKey() + "] Renomeando pasta de scripts";
				String commitMessage = "[RELEASE] Renomeando pasta de scripts";
				gitlabService.atualizaNumeracaoPastaScriptsVersao(gitlabProjectId, branchName, newVersion, actualVersion, commitMessage);
				if(implementsGitflow) {
//					integraReleaseBranchNoBranchMaster(gitlabProjectId, branchName, releaseNotes);
				}
			}
			if(!messages.hasSomeError()) {
				// 2.5 criar tag relacionada à versao que está sendo lançada
//				String tagMessage = "[" + releaseNotes.getIssueKey() + "] Lançamento da versão "+ releaseNotes.getVersion();
				String tagMessage = "[RELEASE] Lançamento da versão "+ releaseNotes.getVersion();
				
				if(StringUtils.isBlank(releaseNotes.getReleaseDate())) {
					String dateStr = Utils.dateToStringPattern(new Date(), JiraService.JIRA_DATETIME_PATTERN);
					releaseNotes.setReleaseDate(dateStr);
				}
				GitlabMarkdown gitlabMarkdown = new GitlabMarkdown();
				releaseNotesModel.setReleaseNotes(releaseNotes);
				String releaseText = releaseNotesModel.convert(gitlabMarkdown);
				messages.info("Lançando a tag: " + newVersion);
//				gitlabService.createVersionTag(gitlabProjectId, newVersion, GitlabService.BRANCH_MASTER, tagMessage, releaseText);
				messages.info("Tag " + newVersion + " lançada");
			}
		}else {
			messages.error("Não encontrado o branch: "+ branchName + " do projeto: " + gitlabProjectId);
		}
	}

}