package dev.pje.bots.apoiadorrequisitante.amqp.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.event.GitlabEventPipeline;
import com.devplatform.model.gitlab.event.GitlabEventPush;
import com.devplatform.model.gitlab.event.GitlabEventPushTag;
import com.devplatform.model.jira.event.JiraEventIssue;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation01TriageHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation02CreateSolutionHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation03CheckAutomaticMergeHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation04ManualMergeHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation05FinishHomologationHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gamification.Gamification010ClassificarAreasConhecimentoHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gamification.Gamification020CalcularPrioridadeDemandaHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.Gitlab010CheckingNewScriptMigrationsInCommitHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.OpenReopenEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.Gitlab040TagPushFinishVersionHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.LabelsChangeEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.PipelineEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.gitlab.Gitlab020GitflowEventHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.jira.Jira010ApoiadorRequisitanteHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.jira.Jira020ClassificationHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.jira.Jira030DemandanteHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.jira.Jira040RaiaFluxoHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.jira.Jira070RegistroAtividadesDemandasHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion010TriageHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion015PrepareActualVersionHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion020GenerateReleaseCandidateHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion030PrepareNextVersionHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion040GenerateReleaseNotesHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion050ProcessReleaseNotesHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion060FinishReleaseNotesProcessingHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion070TagPushedEventHandler;

//@Component
public class AmqpConsumer {

	private static final Logger logger = LoggerFactory.getLogger(AmqpConsumer.class);
    

    private ObjectMapper objectMapper;
	

	private Jira010ApoiadorRequisitanteHandler jira010ApoiadorRequisitanteHandler;


	private Jira020ClassificationHandler jira020ClassificationHandler;
	

	private Jira030DemandanteHandler jira030DemandanteHandler;


	private Jira040RaiaFluxoHandler jira040RaiaFluxoHandler;


	private Jira070RegistroAtividadesDemandasHandler jira070RegistroAtividadesDemandasHandler;

	/**************/

	private Gitlab010CheckingNewScriptMigrationsInCommitHandler gitlab010CheckNewScriptMigrationInCommit;


	private Gitlab020GitflowEventHandler gitlab020Gitflow;


	private OpenReopenEventHandler gitlab030MergeRequestMergeOrClose;


	private Gitlab040TagPushFinishVersionHandler gitlab040TagPushFinishVersion;


	private LabelsChangeEventHandler gitlab060MergeRequestApprovals;


	private PipelineEventHandler gitlab070PipelineFailed;

	/**************/

	private LanVersion010TriageHandler lanversion010;


	private LanVersion015PrepareActualVersionHandler lanversion015;


	private LanVersion020GenerateReleaseCandidateHandler lanversion020;


	private LanVersion030PrepareNextVersionHandler lanversion030;


	private LanVersion040GenerateReleaseNotesHandler lanversion040;


	private LanVersion050ProcessReleaseNotesHandler lanversion050;
	

	private LanVersion060FinishReleaseNotesProcessingHandler lanversion060;
	

	private LanVersion070TagPushedEventHandler lanversion070;

	/**************/

	private Documentation01TriageHandler documentation01;


	private Documentation02CreateSolutionHandler documentation02;


	private Documentation03CheckAutomaticMergeHandler documentation03;
	

	private Documentation04ManualMergeHandler documentation04;
	

	private Documentation05FinishHomologationHandler documentation05;
	
	/***************/

	private Gamification010ClassificarAreasConhecimentoHandler gamification010;

	
	private Gamification020CalcularPrioridadeDemandaHandler gamification020;

	/***************/
	
//	@RabbitListener(
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.default-receive-queue}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-created.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}"})
//		)
	public void jira010Requisitante(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			if(jiraEventIssue.getIssueEventTypeName() == null) {
				logger.error("[REQUISITANTE][JIRA] - " + issueKey + "Falha na identifica????o do tipo de evento");
			}else {
				logger.info("[REQUISITANTE][JIRA] - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				jira010ApoiadorRequisitanteHandler.handle(jiraEventIssue);
			}
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.jira020-classification-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.jira020-classification-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-created.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}"})
//		)
	public void jira020Classification(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			if(jiraEventIssue.getIssueEventTypeName() == null) {
				logger.error("[CLASSIFICACAO][JIRA] - " + issueKey + "Falha na identifica????o do tipo de evento");
			}else {
				logger.info("[CLASSIFICACAO][JIRA] - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				jira020ClassificationHandler.handle(jiraEventIssue);
			}
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.jira030-resposta-demandante.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.jira030-resposta-demandante.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-generic.routing-key}"})
//		)
	public void jira030RespostaDemandante(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			if(jiraEventIssue != null && jiraEventIssue.getIssueEventTypeName() != null) {
				String issueKey = jiraEventIssue.getIssue().getKey();
				logger.info(jira030DemandanteHandler.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				jira030DemandanteHandler.handle(jiraEventIssue);
			}else {
				logger.error(jira030DemandanteHandler.getMessagePrefix() + " - ERRO ao tentar identifiar o evento da mensagem.");
			}
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.jira040-raia-fluxo.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.jira040-raia-fluxo.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-created.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-generic.routing-key}"})
//		)
	public void jira040RaiaFluxo(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			if(jiraEventIssue != null && jiraEventIssue.getIssueEventTypeName() != null) {
				String issueKey = jiraEventIssue.getIssue().getKey();
				logger.info(jira040RaiaFluxoHandler.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				jira040RaiaFluxoHandler.handle(jiraEventIssue);
			}else {
				logger.error(jira040RaiaFluxoHandler.getMessagePrefix() + " - ERRO ao tentar identifiar o evento da mensagem.");
			}
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.jira070-registro-atividades.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.jira070-registro-atividades.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-generic.routing-key}"})
//		)
	public void jira070RegistroAtividadesDemandasHandler(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			if(jiraEventIssue != null && jiraEventIssue.getIssueEventTypeName() != null) {
				String issueKey = jiraEventIssue.getIssue().getKey();
				logger.info(jira070RegistroAtividadesDemandasHandler.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				jira070RegistroAtividadesDemandasHandler.handle(jiraEventIssue);
			}else {
				logger.error(jira070RegistroAtividadesDemandasHandler.getMessagePrefix() + " - ERRO ao tentar identifiar o evento da mensagem.");
			}
		}
	}
	
	/************************/
	// Consumers de gitlab
	/************************/

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab010-commit-script-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab010-commit-script-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.push.routing-key}"})
//		)
	public void gitlab010PushCodeToCheckScript(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventPush gitEventPush = objectMapper.readValue(body, GitlabEventPush.class);
			String projectName = gitEventPush.getProject().getName();
			String branchName = gitEventPush.getRef();
			logger.info("[COMMIT][GITLAB] - project: " + projectName + " branch: " + branchName);
//			gitlab010CheckNewScriptMigrationInCommit.handle(gitEventPush);
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab020-gitflow-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab020-gitflow-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.push.routing-key}"})
//		)
	public void gitlab020GitflowPush(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventPush gitEventPush = objectMapper.readValue(body, GitlabEventPush.class);
			String projectName = gitEventPush.getProject().getName();
			String branchName = gitEventPush.getRef();
			logger.info("[GITFLOW][GITLAB] - project: " + projectName + " branch: " + branchName);
			gitlab020Gitflow.handle(gitEventPush);
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab030-merge-request-merge-close.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab030-merge-request-merge-close.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.merge-request.routing-key}"})
//		)
	public void gitlab030MergeRequestUpdated(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventMergeRequest gitEventMR = objectMapper.readValue(body, GitlabEventMergeRequest.class);
			if(gitEventMR != null) {
				String projectName = gitEventMR.getProject().getName();
				String mergeTitle = "T??tulo n??o identificado";
				if(gitEventMR.getObjectAttributes() != null && StringUtils.isNotBlank(gitEventMR.getObjectAttributes().getTitle())) {
					mergeTitle = gitEventMR.getObjectAttributes().getTitle();
				}
				logger.info(gitlab030MergeRequestMergeOrClose.getMessagePrefix() + " - " + "project: " + projectName + " - MR: " + mergeTitle);
				gitlab030MergeRequestMergeOrClose.handle(gitEventMR);
			}else {
				logger.error(gitlab030MergeRequestMergeOrClose.getMessagePrefix() + " Objeto n??o parece ser de um evento de MR");
			}
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab040-tag-pushed-end-version.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab040-tag-pushed-end-version.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.tag-push.routing-key}"})
//		)
	public void gitlab040TagPushedFinishVersion(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventPushTag gitEventTag = objectMapper.readValue(body, GitlabEventPushTag.class);
			if(gitEventTag != null) {
				String projectName = gitEventTag.getProject().getName();
				String tagName = "T??tulo n??o identificado";
				if(StringUtils.isNotBlank(gitEventTag.getRef())) {
					tagName = gitEventTag.getRef();
				}
				logger.info(gitlab040TagPushFinishVersion.getMessagePrefix() + " - " + "project: " + projectName + " - TAG: " + tagName);
				gitlab040TagPushFinishVersion.handle(gitEventTag);
			}else {
				logger.error(gitlab040TagPushFinishVersion.getMessagePrefix() + " Objeto n??o parece ser de um evento de MR");
			}
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab060-merge-request-approvals.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab060-merge-request-approvals.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.merge-request.routing-key}"})
//		)
	public void gitlab06MergeRequestApprovals(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventMergeRequest gitEventMR = objectMapper.readValue(body, GitlabEventMergeRequest.class);
			if(gitEventMR != null) {
				String projectName = gitEventMR.getProject().getName();
				String mergeTitle = "T??tulo n??o identificado";
				if(gitEventMR.getObjectAttributes() != null && StringUtils.isNotBlank(gitEventMR.getObjectAttributes().getTitle())) {
					mergeTitle = gitEventMR.getObjectAttributes().getTitle();
				}
				logger.info(gitlab060MergeRequestApprovals.getMessagePrefix() + " - " + "project: " + projectName + " - MR: " + mergeTitle);
				gitlab060MergeRequestApprovals.handle(gitEventMR);
			}else {
				logger.error(gitlab060MergeRequestApprovals.getMessagePrefix() + " Objeto n??o parece ser de um evento de MR");
			}
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gitlab070-pipeline-failed.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gitlab070-pipeline-failed.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gitlab.pipeline.routing-key}"})
//		)
	public void gitlab070PipelineFailed(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventPipeline gitEventPipeline = objectMapper.readValue(body, GitlabEventPipeline.class);
			if(gitEventPipeline != null) {
				String projectName = gitEventPipeline.getProject().getName();
				String pipelineRef = null;
				String pipelineId = null;
				String status = null;
				if(gitEventPipeline.getObjectAttributes() != null && StringUtils.isNotBlank(gitEventPipeline.getObjectAttributes().getRef())) {
					pipelineRef = gitEventPipeline.getObjectAttributes().getRef();
				}
				if(gitEventPipeline.getObjectAttributes() != null && gitEventPipeline.getObjectAttributes().getId() != null) {
					pipelineId = gitEventPipeline.getObjectAttributes().getId().toString();
				}
				if(gitEventPipeline.getObjectAttributes() != null && gitEventPipeline.getObjectAttributes().getStatus() != null) {
					status = gitEventPipeline.getObjectAttributes().getStatus().name();
				}
				logger.info(gitlab070PipelineFailed.getMessagePrefix() + " - " + "project: " + projectName + " - Pipeline ref: " + pipelineRef + " - #" + pipelineId + " - status: "+status);
				gitlab070PipelineFailed.handle(gitEventPipeline);
			}else {
				logger.error(gitlab060MergeRequestApprovals.getMessagePrefix() + " Objeto n??o parece ser de um evento de Pipeline");
			}
		}
	}

	/************************/
	// Consumers de lancamento de versao
	/************************/

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver010-triage-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver010-triage-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver010-triage-queue.routing-key-created}", "${spring.rabbitmq.template.custom.lanver010-triage-queue.routing-key-updated}"})
//		)
	public void lanVer010Triage(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion010.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion010.handle(jiraEventIssue);
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver015-prepare-actual-version-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver015-prepare-actual-version-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver015-prepare-actual-version-queue.routing-key-updated}"})
//		)
	public void lanVer015PrepareActualVersion(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion015.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion015.handle(jiraEventIssue);
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver020-release-candidate-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver020-release-candidate-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver020-release-candidate-queue.routing-key-updated}"})
//		)
	public void lanVer020GenerateReleaseCandidate(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion020.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion020.handle(jiraEventIssue);
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver030-next-version-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver030-next-version-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver030-next-version-queue.routing-key-updated}"})
//		)
	public void lanVer030PrepareNextVersion(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion030.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion030.handle(jiraEventIssue);
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver040-release-notes-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver040-release-notes-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver040-release-notes-queue.routing-key-updated}"})
//		)
	public void lanVer040GenerateReleaseNotes(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion040.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion040.handle(jiraEventIssue);
		}
	}	

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver050-version-launch-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver050-version-launch-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver050-version-launch-queue.routing-key-updated}"})
//		)
	public void lanVer050ProcessReleaseNotes(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(lanversion050.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			lanversion050.handle(jiraEventIssue);
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver060-publish-release-notes-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver060-publish-release-notes-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.lanver060-publish-release-notes-queue.routing-key-updated}"})
//		)
	public void lanVer060FinishReleaseNotesProcessing(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			try {
				JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
				String issueKey = jiraEventIssue.getIssue().getKey();
				logger.info(lanversion060.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
				lanversion060.handle(jiraEventIssue);
			}catch (Exception e) {
				logger.error(lanversion060.getMessagePrefix() + " A mensagem recebida n??o ?? compat??vel com o tipo esperado: " + body);
			}
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.lanver070-tag-pushed-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.lanver070-tag-pushed-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC),
//				key = {"${spring.rabbitmq.template.custom.gitlab.tag-push.routing-key}"})
//		)
	public void lanver070TagPushed(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			GitlabEventPushTag gitEventTag = objectMapper.readValue(body, GitlabEventPushTag.class);
			if(gitEventTag != null) {
				String projectName = gitEventTag.getProject().getName();
				String tagName = "T??tulo n??o identificado";
				if(StringUtils.isNotBlank(gitEventTag.getRef())) {
					tagName = gitEventTag.getRef();
				}
				logger.info(lanversion070.getMessagePrefix() + " - " + "project: " + projectName + " - TAG: " + tagName);
				lanversion070.handle(gitEventTag);
			}else {
				logger.error(lanversion070.getMessagePrefix() + " Objeto n??o parece ser de um evento de MR");
			}
		}
	}

	/************************/
	// Consumers de documentacao
	/************************/
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.documentation01-triage-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.documentation01-triage-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.documentation01-triage-queue.routing-key-created}",
//						"${spring.rabbitmq.template.custom.documentation01-triage-queue.routing-key-updated}"})
//		)
	public void documentation01TriageProcessing(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(documentation01.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			documentation01.handle(jiraEventIssue);
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.documentation02-create-solution-queue.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.documentation02-create-solution-queue.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.documentation02-create-solution-queue.routing-key-updated}"})
//		)
	public void documentation02CreateSolution(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(documentation02.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			documentation02.handle(jiraEventIssue);
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.documentation03-check-automatic-merge.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.documentation03-check-automatic-merge.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.documentation03-check-automatic-merge.routing-key-updated}"})
//		)
	public void documentation03CheckAutomaticMerge(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(documentation03.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			documentation03.handle(jiraEventIssue);
		}
	}

//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.documentation04-manual-merge.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.documentation04-manual-merge.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.documentation04-manual-merge.routing-key-updated}"})
//		)
	public void documentation04ManualMerge(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(documentation04.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			documentation04.handle(jiraEventIssue);
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.documentation05-finish-homologation.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.documentation05-finish-homologation.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.documentation05-finish-homologation.routing-key-updated}"})
//		)
	public void documentation05FinishHomologation(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			logger.info(documentation05.getMessagePrefix() + " - " + issueKey + " - " + jiraEventIssue.getIssueEventTypeName().name());
			documentation05.handle(jiraEventIssue);
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gamification010-classificar-areas-conhecimento.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gamification010-classificar-areas-conhecimento.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.gamification010-classificar-areas-conhecimento.routing-key-start}"})
//		)
	public void gamification010ClassificarAreasConhecimento(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			String eventoClassificarAreasConheccimento = objectMapper.readValue(body, String.class);
			logger.info(gamification010.getMessagePrefix() + " - " + msg.getMessageProperties().getReceivedRoutingKey() + " - " + eventoClassificarAreasConheccimento);
			gamification010.handle(eventoClassificarAreasConheccimento);
		}
	}
	
//	@RabbitListener(
//			autoStartup = "${spring.rabbitmq.template.custom.gamification020-calculo-prioridade-demanda.auto-startup}",
//			bindings = @QueueBinding(
//				value = @Queue(value = "${spring.rabbitmq.template.custom.gamification020-calculo-prioridade-demanda.name}", durable = "true", autoDelete = "false", exclusive = "false"), 
//				exchange = @Exchange(value = "${spring.rabbitmq.template.exchange}", type = ExchangeTypes.TOPIC), 
//				key = {"${spring.rabbitmq.template.custom.jira.issue-updated.routing-key}", "${spring.rabbitmq.template.custom.jira.issue-generic.routing-key}"})
//		)
	public void gamification020CalculoPrioridadeGeralDemanda(Message msg) throws Exception {
		if(msg != null && msg.getBody() != null && msg.getMessageProperties() != null) {
			String body = new String(msg.getBody());
			JiraEventIssue jiraEventIssue = objectMapper.readValue(body, JiraEventIssue.class);
			String issueKey = jiraEventIssue.getIssue().getKey();
			String eventType = "undefined";
			if(jiraEventIssue.getIssueEventTypeName() != null) {
				eventType = jiraEventIssue.getIssueEventTypeName().name();
			}
			logger.info(gamification020.getMessagePrefix() + " - " + issueKey + " - " + eventType);
			gamification020.handle(jiraEventIssue);
		}
	}

}