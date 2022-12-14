package dev.pje.bots.apoiadorrequisitante.handlers.jira;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.JiraIssueComment;
import com.devplatform.model.jira.JiraIssueTransition;
import com.devplatform.model.jira.JiraUser;
import com.devplatform.model.jira.event.JiraEventIssue;
import com.devplatform.model.jira.event.JiraWebhookEventEnum;
import com.devplatform.model.jira.request.JiraIssueCreateAndUpdate;

import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.services.TelegramService;

@Component
public class Jira010ApoiadorRequisitanteHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(Jira010ApoiadorRequisitanteHandler.class);

	@Autowired
	private JiraService jiraService;

//	@Autowired
//	private TelegramService telegramService;

	public void handle(JiraEventIssue jiraEventIssue) {
//		telegramService.sendBotMessage("|JIRA||010||REQUISITANTE| - " + jiraEventIssue.getIssue().getKey() + " - " + jiraEventIssue.getIssueEventTypeName().name());
		JiraUser reporter = jiraService.getIssueReporter(jiraEventIssue.getIssue());
		String tribunalUsuario = jiraService.getTribunalUsuario(reporter, true);
		adicionarTribunalRequisitanteDemanda(
				jiraEventIssue.getIssue(), tribunalUsuario, reporter, JiraWebhookEventEnum.ISSUE_CREATED);
		
		JiraUser usuarioAcao = null;
		if(jiraEventIssue.getWebhookEvent() == JiraWebhookEventEnum.ISSUE_UPDATED) {
			if(jiraEventIssue.getComment() != null) {
				if(this.verificaSeRequisitouIssue(jiraEventIssue.getComment())) {
					usuarioAcao = jiraService.getCommentAuthor(jiraEventIssue.getComment());
				}
			}
			if(usuarioAcao == null) {
				usuarioAcao = jiraService.getIssueAssignee(jiraEventIssue.getIssue());
			}
			String tribunalUsuarioAcao = jiraService.getTribunalUsuario(usuarioAcao, true);
			if(StringUtils.isNotBlank(tribunalUsuarioAcao)) {
				tribunalUsuario = tribunalUsuarioAcao;
			}
			adicionarTribunalRequisitanteDemanda(jiraEventIssue.getIssue(), tribunalUsuario, usuarioAcao, JiraWebhookEventEnum.ISSUE_UPDATED);
		}
	}
	
	private void adicionarTribunalRequisitanteDemanda(JiraIssue issue, String tribunal, JiraUser usuario, JiraWebhookEventEnum tipoInclusao) {
		if(tribunal != null) {
			Map<String, Object> updateFields = new HashMap<>();
			try {
				jiraService.adicionaTribunalRequisitante(issue, tribunal, updateFields);
				if(!updateFields.isEmpty()) {
					String linkTribunal = "[" + tribunal +"|" + jiraService.getJqlIssuesPendentesTribunalRequisitante(tribunal) + "]";
					String textoInclusao = "Incluindo "+ linkTribunal +" automaticamente como requisitante desta demanda.";
					if(tipoInclusao.equals(JiraWebhookEventEnum.ISSUE_UPDATED)) {
						textoInclusao = "Incluindo " + linkTribunal +" como requisitante desta demanda de acordo com a participa????o de: [~" + usuario.getName() + "]";
					}
					jiraService.adicionarComentario(issue,textoInclusao, updateFields);
					JiraIssueTransition edicaoAvancada = jiraService.findTransitionByIdOrNameOrPropertyKey(issue, JiraService.TRANSICTION_DEFAULT_EDICAO_AVANCADA);
					if(edicaoAvancada != null) {
						JiraIssueCreateAndUpdate jiraIssueCreateAndUpdate = new JiraIssueCreateAndUpdate();
						jiraIssueCreateAndUpdate.setTransition(edicaoAvancada);
						jiraIssueCreateAndUpdate.setUpdate(updateFields);

						jiraService.updateIssue(issue.getKey(), jiraIssueCreateAndUpdate);
//						telegramService.sendBotMessage("|JIRA||010||REQUISITANTE|[" + issue.getKey() + "] Issue atualizada");
						logger.info("Issue atualizada");
					}else {
//						telegramService.sendBotMessage("*|JIRA||010||REQUISITANTE|[" + issue.getKey() + "] Erro!!* \n N??o h?? transi????o para realizar esta altera????o");
						logger.error("N??o h?? transi????o para realizar esta altera????o");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean verificaSeRequisitouIssue(JiraIssueComment comment) {
		// TODO - verificar se todo coment??rio de algu??m na issue indica que o seu tribunal est?? interessado, ou se ?? necess??rio avaliar alguma express??o espec??fica
		// TODO - o ideal ?? treinar um modelo de ML com os coment??rios que significam solicitar interesse na demanda
		return comment.getBody().toLowerCase().contains("atribuir") || 
				comment.getBody().toLowerCase().contains("interesse");
	}
}