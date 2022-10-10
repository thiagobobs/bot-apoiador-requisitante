package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabMergeRequestActionsEnum;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.jira.JiraIssueTransition;
import com.devplatform.model.jira.JiraIssueTransitions;
import com.devplatform.model.jira.JiraVersion;
import com.devplatform.model.jira.request.JiraIssueCreateAndUpdate;

import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;

@Component
public class MergeEventHandler extends Handler<GitlabEventMergeRequest> {

	@Autowired
	private Gitlab010CheckingNewScriptMigrationsInCommitHandler gitlab010CheckNewScriptMigrationInCommit;

	private static final Logger logger = LoggerFactory.getLogger(MergeEventHandler.class);

	private static final Map<String, String> STATUS_ID = new HashMap<>();
	
	static {
		STATUS_ID.put("PJEII", "2261");
		STATUS_ID.put("PJEVII", "2261");
		STATUS_ID.put("PJEWEB", "2261");
		STATUS_ID.put("PJECRI", "371");
		STATUS_ID.put("SISBAJUD", "121"); // Transição: Aprovar entrega
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "[GITLAB]-[MERGE-REQUEST]-[MERGE]";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Override
	public void handle(GitlabEventMergeRequest event) {
		if (GitlabMergeRequestActionsEnum.MERGE.equals(event.getObjectAttributes().getAction()) && !gitlabService.isBranchRelease(event.getObjectAttributes().getSourceBranch())) {
			String email = event.getUser().getEmail().equals("[REDACTED]") ? gitlabService.findUserByUsername(event.getUser().getUsername()).getEmail() : event.getUser().getEmail();

			String jiraComment = String.format("MR#%s integrado ao branch %s pelo usuário %s",
					event.getObjectAttributes().getIid().toString(), event.getObjectAttributes().getTargetBranch(), email);

			String issueKey = Utils.getIssueKey(event.getObjectAttributes().getLastCommit().getMessage());
			logger.info("Issuekey: {}", issueKey);

			String jiraProjectKey = issueKey.substring(0, issueKey.indexOf("-"));
			logger.info("Transition: {}", STATUS_ID.get(jiraProjectKey));

			logger.info("Issue Status Name: {}", this.jiraService.recuperaIssue(issueKey, Collections.singletonMap("fields", JiraService.FIELD_STATUS)).getFields().getStatus().getName());

			JiraIssueTransitions jiraIssueTransitions = this.jiraService.recuperarTransicoesIssue(issueKey);
			logger.info("Transições disponíveis: {}", jiraIssueTransitions != null ? jiraIssueTransitions.getTransitions().stream().map(JiraIssueTransition::getId).collect(Collectors.joining(", ")) : "");
			if (jiraIssueTransitions != null && jiraIssueTransitions.getTransitions().stream().anyMatch(p -> p.getId().equals(STATUS_ID.get(jiraProjectKey)))) {
				logger.info("Transition Issue to {}", STATUS_ID.get(jiraProjectKey));
				this.jiraService.updateIssue(issueKey, this.getIssueUpdate(event, jiraComment, STATUS_ID.get(jiraProjectKey), jiraProjectKey));
			} else {
				logger.info("Send Comment to Issue");
				this.jiraService.sendTextAsComment(issueKey, jiraComment);
			}

			this.gitlab010CheckNewScriptMigrationInCommit.handle(event);

			this.gitlabService.closeMergeRequestThatCannotBeMerged(event.getProject().getId().toString());
		}
	}

	private JiraIssueCreateAndUpdate getIssueUpdate(GitlabEventMergeRequest event, String jiraComment, String idTransition, String jiraProjectKey) {
		JiraIssueCreateAndUpdate issueUpdate = new JiraIssueCreateAndUpdate();

		issueUpdate.setFields(this.getFields(event, jiraProjectKey));
		issueUpdate.setTransition(Collections.singletonMap("id", idTransition));
		issueUpdate.setUpdate(Collections.singletonMap("comment", Collections.singletonList(Collections.singletonMap("add", Collections.singletonMap("body", jiraComment)))));

		return issueUpdate;
	}

	private Map<String, Object> getFields(GitlabEventMergeRequest event, String jiraProjectKey) {
		Map<String, Object> fields = new HashMap<>();

		fields.put(JiraService.FIELD_RESOLUTION, Collections.singletonMap("name", "Resolvido"));
		fields.put(JiraService.FIELD_MRS_ACEITOS, event.getObjectAttributes().getUrl());

		String appVersion = gitlabService.getVersion(event.getProject().getId().toString(), event.getObjectAttributes().getTargetBranch(), true);
		
		JiraVersion jiraVersion = null;
		if (appVersion != null) {
			jiraVersion = jiraService.createProjectVersionIfNotExists(jiraProjectKey, appVersion, "Contempla as demandas relacionadas à versão " + appVersion);	
		}
		
		if (jiraVersion != null) {
			fields.put(JiraService.FIELD_FIX_VERSION, Collections.singletonList(Collections.singletonMap("name", appVersion)));
		}

		return fields;
	}

}
