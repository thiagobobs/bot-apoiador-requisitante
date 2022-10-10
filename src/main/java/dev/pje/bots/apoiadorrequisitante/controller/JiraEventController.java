package dev.pje.bots.apoiadorrequisitante.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.devplatform.model.jira.JiraUserCreate;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class JiraEventController {

	private static final Logger logger = LoggerFactory.getLogger(JiraEventController.class);

	@Autowired
	private ObjectMapper objectMapper;

	@PostMapping(value = "/jira/user-create")
	public void process(@RequestBody String json) {
		try {
			JiraUserCreate jiraUserCreate = objectMapper.readValue(json, JiraUserCreate.class);
			logger.info("[JIRA][USER CREATE] {} {}", jiraUserCreate.getUser().getName(), jiraUserCreate.getUser().getEmailAddress());	
		} catch (Exception ex) {
			logger.error(ex.getLocalizedMessage(), ex);
		}
	}

}
