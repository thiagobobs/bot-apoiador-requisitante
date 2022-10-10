package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabPipelineStatusEnum;
import com.devplatform.model.gitlab.event.GitlabEventPipeline;

import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;

@Component
public class PipelineEventHandler extends Handler<GitlabEventPipeline>{

	private static final Logger logger = LoggerFactory.getLogger(PipelineEventHandler.class);

	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	@Override
	public String getMessagePrefix() {
		return "|GITLAB||PIPELINE-EVENT|";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Override
	public void handle(GitlabEventPipeline gitlabEventPipeline) {
		messages.clean();

		if (gitlabEventPipeline.getMergeRequest() != null && GitlabPipelineStatusEnum.statusFailed(gitlabEventPipeline.getObjectAttributes().getStatus())) {
			this.gitlabService.sendMergeRequestComment(gitlabEventPipeline.getProject().getId().toString(), gitlabEventPipeline.getMergeRequest().getIid(), "O processamento do CI/CD não encerrou corretamente");
			this.gitlabService.closeMergeRequest(gitlabEventPipeline.getProject().getId().toString(), gitlabEventPipeline.getMergeRequest().getIid());
		}
	}
}
