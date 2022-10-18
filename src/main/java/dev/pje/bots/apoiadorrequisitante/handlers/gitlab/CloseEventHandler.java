package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabMergeRequestActionsEnum;
import com.devplatform.model.gitlab.GitlabNote;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.jira.JiraIssueTransition;
import com.devplatform.model.jira.JiraIssueTransitions;
import com.devplatform.model.jira.JiraUser;
import com.devplatform.model.jira.request.JiraIssueCreateAndUpdate;

import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;

@Component
public class CloseEventHandler extends Handler<GitlabEventMergeRequest> {

	private static final Logger logger = LoggerFactory.getLogger(CloseEventHandler.class);

	private static final Map<String, String> STATUS_ID = new HashMap<>();

	static {
		STATUS_ID.put("PJEII", "2131");
		STATUS_ID.put("PJEVII", "2131");
		STATUS_ID.put("PJEWEB", "2131");
		STATUS_ID.put("PJECRI", "321");
		STATUS_ID.put("PJEMNICLI", "321");
		STATUS_ID.put("SISBAJUD", "351"); // Transição: Rejeitar Entrega
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "[GITLAB]-[MERGE-REQUEST]-[CLOSE]";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Override
	public void handle(GitlabEventMergeRequest event) {
		if (GitlabMergeRequestActionsEnum.CLOSE.equals(event.getObjectAttributes().getAction())) {
			String email = event.getUser().getEmail().equals("[REDACTED]") ? gitlabService.findUserByUsername(event.getUser().getUsername()).getEmail() : event.getUser().getEmail();
			
			List<GitlabNote> gitLabNotes = this.gitlabService.listAllMergeRequestNotes(event.getProject().getId().toString(), event.getObjectAttributes().getIid());

			String jiraComment = gitLabNotes.isEmpty() || !email.equals("bot.revisor.pje@cnj.jus.br") ? 
					"[MR#" + event.getObjectAttributes().getIid() + "|" + event.getObjectAttributes().getUrl() + "] fechado pelo usuário " + email : 
							"[MR#" + event.getObjectAttributes().getIid() + "|" + event.getObjectAttributes().getUrl() + "] fechado. " + gitLabNotes.get(0).getBody();

			String issueKey = event.getObjectAttributes().getLastCommit() != null ? Utils.getIssueKey(event.getObjectAttributes().getLastCommit().getMessage()) : null;
			logger.info("Issuekey: {}", issueKey);

			String jiraProjectKey = issueKey != null ? issueKey.substring(0, issueKey.indexOf("-")) : StringUtils.EMPTY;
			logger.info("Transition: {}", STATUS_ID.get(jiraProjectKey));

			if (issueKey != null) {
				logger.info("Issue Status Name: {}", this.jiraService.recuperaIssue(issueKey, Collections.singletonMap("fields", JiraService.FIELD_STATUS)).getFields().getStatus().getName());
				
				JiraIssueTransitions jiraIssueTransitions = this.jiraService.recuperarTransicoesIssue(issueKey);
				logger.info("Transições disponíveis: {}", jiraIssueTransitions != null ? jiraIssueTransitions.getTransitions().stream().map(JiraIssueTransition::getId).collect(Collectors.joining(", ")) : "");
				if (jiraIssueTransitions != null && jiraIssueTransitions.getTransitions().stream().anyMatch(p -> p.getId().equals(STATUS_ID.get(jiraProjectKey)))) {
					logger.info("Transition Issue to {}", STATUS_ID.get(jiraProjectKey));
					JiraUser jiraUser = this.jiraService.recuperaIssue(issueKey, Collections.singletonMap("fields", JiraService.FIELD_RESPONSAVEL_CODIFICACAO)).getFields().getResponsavelCodificacao();
					String responsavelCodificacao = jiraUser != null ? jiraUser.getName() : StringUtils.EMPTY;
					logger.info("Responsável pela codificação: {}", responsavelCodificacao);
					this.jiraService.updateIssue(issueKey, this.getIssueUpdate(jiraComment, STATUS_ID.get(jiraProjectKey), responsavelCodificacao));
				} else {
					logger.info("Send Comment to Issue");
					this.jiraService.sendTextAsComment(issueKey, jiraComment);
				}
			}
		}
	}

	private JiraIssueCreateAndUpdate getIssueUpdate(String jiraComment, String idTransition, String userName) {
		JiraIssueCreateAndUpdate issueUpdate = new JiraIssueCreateAndUpdate();

		issueUpdate.setTransition(Collections.singletonMap("id", idTransition));
		issueUpdate.setUpdate(Collections.singletonMap(JiraService.FIELD_COMMENT, Collections.singletonList(Collections.singletonMap("add", Collections.singletonMap("body", jiraComment)))));

		Map<String, Object> fields = new HashMap<>();
		fields.put(JiraService.FIELD_USUARIOS_RESPONSAVEIS_REVISAO, Collections.emptyList());
		if (StringUtils.isNotEmpty(userName)) {
			fields.put(JiraService.FIELD_RESPONSAVEL_ISSUE, Collections.singletonMap("name", userName));
		}
		issueUpdate.setFields(fields);

		return issueUpdate;
	}

}
