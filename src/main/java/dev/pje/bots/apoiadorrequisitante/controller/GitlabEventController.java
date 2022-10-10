package dev.pje.bots.apoiadorrequisitante.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.event.GitlabEventPipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.CloseEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.LabelsChangeEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.MergeEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.OpenReopenEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.PipelineEventHandler;
import dev.pje.bots.apoiadorrequisitante.services.GitlabService;

@RestController()
public class GitlabEventController {
	
	private static final Logger logger = LoggerFactory.getLogger(GitlabEventController.class);
	
	@Autowired
	private ObjectMapper objectMapper;
	
//	@Autowired
//	private Gitlab010CheckingNewScriptMigrationsInCommitHandler gitlab010CheckNewScriptMigrationInCommit;
	
	@Autowired
	private OpenReopenEventHandler openReopenEventHandler;

	@Autowired
	private MergeEventHandler mergeEventHandler;

	@Autowired
	private CloseEventHandler closeEventHandler;
	
	@Autowired
	private LabelsChangeEventHandler labelsChangeEventHandler;
	
	@Autowired
	private PipelineEventHandler pipelineEventHandler;
	
	@Autowired
	private GitlabService gitlabService;

	@PostMapping(value = "/event")
	public void process(@RequestHeader("X-Gitlab-Event") String eventType, @RequestBody String inputJson) throws JsonProcessingException {
		logger.info("Event type: {}", eventType);

//		if (eventType.contains("Push")) {
//			GitlabEventPush event = objectMapper.readValue(inputJson, GitlabEventPush.class);
//			logger.info("[GITLAB][PUSH] -> PROJECT {} | BRANCH {}", event.getProject().getName(), event.getRef());
//			
//			this.gitlab010CheckNewScriptMigrationInCommit.handle(event);
//		} else 
		if (eventType.contains("Merge Request")) {
			GitlabEventMergeRequest event = objectMapper.readValue(inputJson, GitlabEventMergeRequest.class);
			logger.info("[GITLAB][MERGE REQUEST] -> PROJECT {} | MR#{} | ACTION {}", event.getProject().getName(), event.getObjectAttributes().getIid(), event.getObjectAttributes().getAction().name());
			
			this.openReopenEventHandler.handle(event);
			this.labelsChangeEventHandler.handle(event);
			this.mergeEventHandler.handle(event);
			this.closeEventHandler.handle(event);
			
		} else if (eventType.contains("Pipeline")) {
			GitlabEventPipeline event = objectMapper.readValue(inputJson, GitlabEventPipeline.class);
			logger.info("[GITLAB][PIPELINE] -> PROJECT {} | BRANCH {}", event.getProject().getName(), event.getObjectAttributes().getRef());
			
			this.pipelineEventHandler.handle(event);
		}
	}

	@PutMapping(value = "/verify/{id}")
	public void verify(@PathVariable String id) {
		this.gitlabService.closeMergeRequestThatCannotBeMerged(id);
	}

	
}
