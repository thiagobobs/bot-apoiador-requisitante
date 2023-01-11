package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabMergeRequestStateEnum;
import com.devplatform.model.gitlab.event.GitlabEventPush;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;

@Component
public class PushEventHandler extends Handler<GitlabEventPush> {
	
	private static final Logger logger = LoggerFactory.getLogger(PushEventHandler.class);

	@Override
	protected Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessagePrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLogLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void handle(GitlabEventPush event) {
		try {
			logger.info("GitlabEventPush: " + new ObjectMapper().writeValueAsString(event));
		} catch (JsonProcessingException ex) {
			logger.error(ex.getLocalizedMessage());
		}
		String email = StringUtils.isNotEmpty(event.getUserEmail()) ? event.getUserEmail() : gitlabService.findUserById(event.getUserId().toString()).getEmail();

		logger.info("Usuário autor do push: {}", email);
		if (email != null && email.equals("bot.revisor.pje@cnj.jus.br")) {
			logger.info("Após rebase, aprova MR para o branch {}", event.getRef().substring(event.getRef().lastIndexOf("/") + 1));
			
			Map<String, String> options = new HashMap<>();
			options.put("state", GitlabMergeRequestStateEnum.OPENED.toString());
			options.put("source_branch", event.getRef().substring(event.getRef().lastIndexOf("/") + 1));
			options.put("target_branch", event.getProject().getDefaultBranch());

			List<GitlabMRResponse> mergeRequests = gitlabService.findMergeRequests(event.getProject().getId().toString(), options);
			if (!mergeRequests.isEmpty()) {
				GitlabMRResponse mrOpened = mergeRequests.get(0);
				try {
					this.gitlabService.acceptMergeRequest(event.getProject().getId().toString(), mrOpened.getIid(), true, true);
				} catch (GitlabException ex) {
					this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), mrOpened.getIid(), ex.getLocalizedMessage());
				}
			} else {
				logger.error("Nenhum MR encontrado com os parâmetros: state {}, source_branch {}, target_branch {}", 
						GitlabMergeRequestStateEnum.OPENED.toString(), event.getRef().substring(event.getRef().lastIndexOf("/") + 1), event.getProject().getDefaultBranch());
			}
		}
		
	}

}
