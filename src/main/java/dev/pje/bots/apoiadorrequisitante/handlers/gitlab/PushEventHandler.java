package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devplatform.model.gitlab.GitlabMergeRequestStateEnum;
import com.devplatform.model.gitlab.event.GitlabEventPush;
import com.devplatform.model.gitlab.response.GitlabMRResponse;

import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;

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
		String email = event.getCommits().get(0).getAuthor().getEmail();

		if (!email.equals("bot.revisor.pje@cnj.jus.br")) {
			logger.info("Após rebase, aprova MR para o branch {}", event.getRef().substring(event.getRef().lastIndexOf("/")));
			
			Map<String, String> options = new HashMap<>();
			options.put("state", GitlabMergeRequestStateEnum.OPENED.toString());
			options.put("source_branch", event.getRef().substring(event.getRef().lastIndexOf("/")));
			options.put("target_branch", event.getProject().getDefaultBranch());

			List<GitlabMRResponse> mergeRequests = gitlabService.findMergeRequests(event.getProject().getId().toString(), options);
			if (!mergeRequests.isEmpty()) {
				GitlabMRResponse mrOpened = mergeRequests.get(0);
				try {
					this.gitlabService.acceptMergeRequest(event.getProject().getId().toString(), mrOpened.getIid());
				} catch (GitlabException ex) {
					this.gitlabService.sendMergeRequestComment(event.getProject().getId().toString(), mrOpened.getIid(), ex.getLocalizedMessage());
				}
			} else {
				logger.error("Nenhum MR encontrado com os parâmetros: state {}, source_branch {}, target_branch {}", 
						GitlabMergeRequestStateEnum.OPENED.toString(), event.getRef().substring(event.getRef().lastIndexOf("/")), event.getProject().getDefaultBranch());
			}
		}
		
	}

}
