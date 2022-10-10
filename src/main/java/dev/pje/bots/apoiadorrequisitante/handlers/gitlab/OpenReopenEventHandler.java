package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabChanges;
import com.devplatform.model.gitlab.GitlabMergeRequestActionsEnum;
import com.devplatform.model.gitlab.GitlabMergeRequestStatusEnum;
import com.devplatform.model.gitlab.GitlabMergeRequestPipeline;
import com.devplatform.model.gitlab.GitlabPipelineStatusEnum;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.response.GitlabCommitResponse;
import com.devplatform.model.gitlab.response.GitlabMRChanges;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.devplatform.model.gitlab.vo.GitlabScriptVersaoVO;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.request.JiraIssueCreateAndUpdate;

import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.GitlabService;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;

@Component
public class OpenReopenEventHandler extends Handler<GitlabEventMergeRequest> {

	private static final Logger logger = LoggerFactory.getLogger(OpenReopenEventHandler.class);

	private static final Map<String, IssueStatus> STATUS_ID = new HashMap<>();

	static {
		STATUS_ID.put("PJEII", new IssueStatus(BigDecimal.valueOf(3), "EM PROGRESSO", "1931"));
		STATUS_ID.put("PJEVII", new IssueStatus(BigDecimal.valueOf(3), "EM PROGRESSO", "1931"));
		STATUS_ID.put("PJEWEB", new IssueStatus(BigDecimal.valueOf(3), "EM PROGRESSO", "1931"));
		STATUS_ID.put("PJECRI", new IssueStatus(BigDecimal.valueOf(10392), "FAZENDO", "271"));
		STATUS_ID.put("SISBAJUD", new IssueStatus(BigDecimal.valueOf(3), "EM PROGRESSO", "381"));
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "[GITLAB]-[MERGE-REQUEST]-[OPEN|REOPEN]";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}
	
	private String issueKeyCommitMessage;
	private String issueKeyMergeTitle;
	private JiraIssue jiraIssue;

	@Override
	public void handle(GitlabEventMergeRequest event) {
		String email = event.getUser().getEmail().equals("[REDACTED]") ? gitlabService.findUserByUsername(event.getUser().getUsername()).getEmail() : event.getUser().getEmail();

		if (!email.equals("bot.revisor.pje@cnj.jus.br") && 
				GitlabMergeRequestActionsEnum.OPEN.equals(event.getObjectAttributes().getAction()) || GitlabMergeRequestActionsEnum.REOPEN.equals(event.getObjectAttributes().getAction())) {

			ValidationResult validationResult = new CommitMessageValidator()
					.next(new MergeRequestTitleValidator()
							.next(new TargetBranchValidator()
									.next(new MergeRequestStatusValidator()
											.next(new IssueInfoValidator()
													.next(new FileNameValidator())))))
					.verify(event);

			if (!validationResult.isValid()) {
				this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), event.getObjectAttributes().getIid(), validationResult.getErrorMsg());
				this.gitlabService.closeMergeRequest(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
			} else {
				this.executaPipeline(event);

				this.gitlabService.atualizaLabelsMR(event.getObjectAttributes(), Collections.singletonList(jiraIssue.getFields().getIssuetype().getName()));

				JiraIssueCreateAndUpdate issueUpdate = new JiraIssueCreateAndUpdate();
				if (STATUS_ID.get(jiraIssue.getFields().getProject().getKey()) != null && jiraIssue.getFields().getStatus().getId().equals(STATUS_ID.get(jiraIssue.getFields().getProject().getKey()).id)) {
					logger.info("Transition id: {}", STATUS_ID.get(jiraIssue.getFields().getProject().getKey()).transitionId);
					issueUpdate.setTransition(Collections.singletonMap("id", STATUS_ID.get(jiraIssue.getFields().getProject().getKey()).transitionId));
				} 

				String jiraComment = String.format("[MR#%s|%s] %s pelo usuário %s", 
						event.getObjectAttributes().getIid().toString(), event.getObjectAttributes().getUrl(), event.getObjectAttributes().getAction().translated, email);
				
				issueUpdate.setUpdate(Collections.singletonMap("comment", Collections.singletonList(Collections.singletonMap("add", Collections.singletonMap("body", jiraComment)))));
				issueUpdate.setFields(Collections.singletonMap("assignee", Collections.singletonMap("name", "revisao.tecnia.pje")));

				this.jiraService.updateIssue(issueKeyCommitMessage, issueUpdate);
			}
		}

	}

	private void executaPipeline(GitlabEventMergeRequest event) {
		List<GitlabMergeRequestPipeline> pipelines = gitlabService.listMergePipelines(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
		if (!pipelines.isEmpty() && GitlabPipelineStatusEnum.statusFailed(pipelines.get(0).getStatus())) {
			this.gitlabService.createMRPipeline(event.getProject().getId(), event.getObjectAttributes().getIid());
		}
	}

	private class CommitMessageValidator extends ValidationChain<GitlabEventMergeRequest>{

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {
			List<GitlabCommitResponse> commits = gitlabService.getCommits(event.getProject().getId(), event.getObjectAttributes().getIid());

			for (GitlabCommitResponse commit : commits) {
				issueKeyCommitMessage = Utils.getIssueKey(commit.getMessage());

				if (issueKeyCommitMessage == null) {
					return ValidationResult.invalid("A mensagem de commit está fora do padrão. O correto é [IDENTIFICADOR DA ISSUE] Título");
				}
			}

			jiraIssue = jiraService.recuperaIssueDetalhada(issueKeyCommitMessage);

			if (jiraIssue == null) {
				return ValidationResult.invalid(String.format("O identificador da issue (%s) especificado na mensagem de commit não corresponde a uma issue válida no Jira", issueKeyCommitMessage));
			}

			String jiraProjectKey = gitlabService.getJiraProjectKey(event.getProject().getId().toString());

			if (jiraProjectKey != null && !issueKeyCommitMessage.contains(jiraProjectKey)) {
				return ValidationResult.invalid(String.format("Somente MRs relacionados ao projeto do Jira %s podem ser abertos no projeto %s", jiraProjectKey, event.getProject().getName()));
			}

			return verifyNext(event);
		}

	}

	private class MergeRequestTitleValidator extends ValidationChain<GitlabEventMergeRequest> {

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {

			issueKeyMergeTitle = Utils.getIssueKey(event.getObjectAttributes().getTitle());

			if (issueKeyMergeTitle == null) {
				return ValidationResult.invalid("O título do MR está fora do padrão. O correto é [IDENTIFICADOR DA ISSUE] Título");
			}

			if (!issueKeyMergeTitle.equals(issueKeyCommitMessage)) {
				return ValidationResult.invalid(String.format("O identificador da issue informado no título do MR (%s) não é igual ao identificador da issue informado na mensagem de commit (%s)", issueKeyMergeTitle, issueKeyCommitMessage));
			}

			return verifyNext(event);
		}

	}

	private class TargetBranchValidator extends ValidationChain<GitlabEventMergeRequest> {

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {
			List<String> tipos = Arrays.asList("Defeito", "Melhoria", "Nova funcionalidade");
			String issueType = jiraIssue.getFields().getIssuetype().getName();

			if (tipos.contains(issueType) && !event.getObjectAttributes().getTargetBranch().equals(event.getProject().getDefaultBranch())) {
				return ValidationResult.invalid(String.format("Target branch incorreto. Para o tipo de issue %s apenas o branch %s será aceito", issueType, event.getProject().getDefaultBranch()));
			}

			return verifyNext(event);
		}

	}

	private class MergeRequestStatusValidator extends ValidationChain<GitlabEventMergeRequest> {

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {

			if (GitlabMergeRequestStatusEnum.CAN_NOT_BE_MERGED.equals(event.getObjectAttributes().getMergeStatus())) {
				return ValidationResult.invalid(String.format("O código não pode ser integrado ao branch %s pois apresenta conflitos.", event.getObjectAttributes().getTargetBranch()));
			}

			GitlabMRResponse gitlabMRResponse = gitlabService.getMergeRequest(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
			if (gitlabMRResponse.getDivergedCommitsCount() != 0) {
				return ValidationResult.invalid(String.format("O branch %s está %s commit(s) atrás do branch %s. Favor realizar o rebase", 
						event.getObjectAttributes().getSourceBranch(), gitlabMRResponse.getDivergedCommitsCount(), event.getObjectAttributes().getTargetBranch()));				
			}

			return verifyNext(event);
		}

	}

	private class IssueInfoValidator extends ValidationChain<GitlabEventMergeRequest> {

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {

			if (jiraIssue.getFields().getResponsavelCodificacao() == null || !event.getObjectAttributes().getLastCommit().getAuthor().getEmail().equalsIgnoreCase(jiraIssue.getFields().getResponsavelCodificacao().getEmailAddress())) {
				return ValidationResult.invalid(String.format("Há divergência entre o usuário autor do commit (%s) e o usuário cadastrado na issue como responsável pela codificação (%s)", event.getObjectAttributes().getLastCommit().getAuthor().getEmail(), jiraIssue.getFields().getResponsavelCodificacao() == null ? "NULL" : jiraIssue.getFields().getResponsavelCodificacao().getEmailAddress()));
			}

			logger.info("[IssueInfoValidator] Status id: {} | Status name: {}", jiraIssue.getFields().getStatus().getId(), jiraIssue.getFields().getStatus().getName());
			if (STATUS_ID.get(jiraIssue.getFields().getProject().getKey()) != null && !jiraIssue.getFields().getStatus().getId().equals(STATUS_ID.get(jiraIssue.getFields().getProject().getKey()).id)) {
				return ValidationResult.invalid(String.format("A issue não está com o status correto. Status válido ao abrir um MR é '%s'", STATUS_ID.get(jiraIssue.getFields().getProject().getKey()).name));
			}

			return verifyNext(event);
		}

	}

	private class FileNameValidator extends ValidationChain<GitlabEventMergeRequest> {

		@Override
		public ValidationResult verify(GitlabEventMergeRequest event) {
			if (event.getProject().getId().equals(BigDecimal.valueOf(7))) {
				List<String> files = new ArrayList<>();

				GitlabMRChanges gitlabMRChanges = gitlabService.getMergeRequestChanges(event.getProject().getId().toString(), event.getObjectAttributes().getIid());

				if(gitlabMRChanges != null && !gitlabMRChanges.getChanges().isEmpty()) {
					for (GitlabChanges changes : gitlabMRChanges.getChanges()) {
						if(BooleanUtils.isTrue(changes.getNewFile()) && changes.getNewPath().toLowerCase().endsWith(GitlabService.SCRIPT_EXTENSION)) {
							try {
								new GitlabScriptVersaoVO(changes.getNewPath());
							} catch (IllegalArgumentException ex) {
								files.add(changes.getNewPath());
							}
						}
					}
					if (!files.isEmpty()) {
						return ValidationResult.invalid(String.format("Nome do(s) arquivo(s) de script %s inválido(s)", files.stream().map(Object::toString).collect(Collectors.joining(", "))));
					}
				}
			}

			return verifyNext(event);
		}
		
	}

	private static class IssueStatus {
		BigDecimal id;
		String name;
		String transitionId;

		public IssueStatus(BigDecimal id, String name, String transitionId) {
			this.id = id;
			this.name = name;
			this.transitionId = transitionId;
		}
	}

}
