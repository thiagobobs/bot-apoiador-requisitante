package dev.pje.bots.apoiadorrequisitante.utils.textModels;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.VersionReleaseNotes;

import dev.pje.bots.apoiadorrequisitante.utils.JiraUtils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.MarkdownInterface;

@Component
public class NewVersionReleasedSimpleCallTextModel implements AbstractTextModel{
	
	@Value("${clients.jira.url}")
	private String JIRAURL;

	private VersionReleaseNotes releaseNotes;
	
	public void setReleaseNotes(VersionReleaseNotes releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public String convert(MarkdownInterface markdown) {
		
		StringBuilder markdownText = new StringBuilder();
		if(releaseNotes != null && releaseNotes.getVersion() != null && releaseNotes.getVersionType() != null && StringUtils.isNotBlank(releaseNotes.getProject())) {
			
			releaseNotes.setProject("pje");
			
//			if(releaseNotes.getProject().contains("legay") || Utils.compareAsciiIgnoreCase(releaseNotes.getProject(), "PJE")) {
//				markdownText.append(markdown.bold("Atualize o PJe-Legacy do seu tribunal!"))
//					.append(markdown.newLine());
//			}
			StringBuilder titleSb = new StringBuilder();
			titleSb.append("Versão ").append(releaseNotes.getVersion());
//			if(releaseNotes.getVersionType().equalsIgnoreCase("hotfix")) {
//				titleSb.append(" (").append(releaseNotes.getVersionType()).append(")");
//			}
			titleSb.append(" do projeto ").append(releaseNotes.getProject()).append(" disponibilizada");
			markdownText.append(markdown.head3(titleSb.toString()))
				.append(markdown.normal("Acesse o release notes desta versão " + markdown.link(releaseNotes.getUrl() + 
						"servicos-negociais/servico-pje-legacy/notas-da-versao", "aqui") + "."));

		}
		
		return markdownText.toString();
	}
	
	private String getPathJql(String jql) {
		return JiraUtils.getPathJql(jql, JIRAURL);
	}
}
