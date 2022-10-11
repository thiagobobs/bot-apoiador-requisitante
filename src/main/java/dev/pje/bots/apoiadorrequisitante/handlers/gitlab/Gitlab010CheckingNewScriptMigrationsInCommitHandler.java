package dev.pje.bots.apoiadorrequisitante.handlers.gitlab;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.devplatform.model.gitlab.GitlabChanges;
import com.devplatform.model.gitlab.GitlabProject;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.response.GitlabMRChanges;
import com.devplatform.model.gitlab.response.GitlabRepositoryTree;
import com.devplatform.model.gitlab.vo.GitlabScriptVersaoVO;

import dev.pje.bots.apoiadorrequisitante.services.GitlabService;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;

@Component
public class Gitlab010CheckingNewScriptMigrationsInCommitHandler {

	private static final Logger logger = LoggerFactory.getLogger(Gitlab010CheckingNewScriptMigrationsInCommitHandler.class);

	@Autowired
	private GitlabService gitlabService;

	public void handle(GitlabEventMergeRequest event) {
		if(event != null) {
			String branchName = event.getObjectAttributes().getTargetBranch();

			if(GitlabService.BRANCH_DEVELOP.equals(branchName) && event.getProject().getId().equals(BigDecimal.valueOf(7))) { // Somente para o projeto pje
				logger.info("Iniciando operação de reordenação de scripts");
//				String lastCommitId = gitlabEventPush.getCommits().get(0).getId();
				String issueKey = Utils.getIssueKeyFromCommitMessage(event.getObjectAttributes().getLastCommit().getMessage());

				List<String> addedScripts = getAddedScripts(event.getProject().getId().toString(), event.getObjectAttributes().getIid());
				List<GitlabScriptVersaoVO> scriptsToChange = new ArrayList<>();
				if(addedScripts != null && !addedScripts.isEmpty()) {
					// identificar qual é o número da versão atual
					String currentVersion = gitlabService.getVersion(event.getProject().getId(), branchName, true);
					if(StringUtils.isNotBlank(currentVersion)) {
						// verificar se todos os arquivos adicionados estão no caminho correto
						String destinationPath = GitlabService.SCRIPS_MIGRATION_BASE_PATH + currentVersion;
						// buscar todos os scripts que estao na pasta de migrations
						List<GitlabRepositoryTree> currentScriptList = gitlabService.getFilesFromPath(event.getProject().getId(), branchName, destinationPath);
						
						boolean scriptsOutOfOrder = false;
						for (String addedScript : addedScripts) {
							GitlabScriptVersaoVO addedScriptObj = new GitlabScriptVersaoVO(addedScript);
							if(addedScript.startsWith(GitlabService.SCRIPS_MIGRATION_BASE_PATH)) {
								if(!addedScript.startsWith(destinationPath)) {
									scriptsToChange.add(addedScriptObj);
								}else{
									// check if some added script is in the same order of a current script
									if(addedScriptObj.getSpecificName() != null && !currentScriptList.isEmpty()) {
										for (GitlabRepositoryTree elmentFromTree : currentScriptList) {
											GitlabScriptVersaoVO currentScriptObj = new GitlabScriptVersaoVO(elmentFromTree.getPath());
											if(!currentScriptObj.getName().equals(addedScriptObj.getName())) {
												if(addedScriptObj.getVersion().equals(currentScriptObj.getVersion()) && addedScriptObj.getOrder().equals(currentScriptObj.getOrder())) {
													scriptsOutOfOrder = true;
													break;
												}
											}
										}
										if(scriptsOutOfOrder) {
											break;
										}
									}
								}
							}
						}
						if(scriptsOutOfOrder) {
							scriptsToChange = new ArrayList<>();
							for (String addedScript : addedScripts) {
								GitlabScriptVersaoVO addedScriptObj = new GitlabScriptVersaoVO(addedScript);
								scriptsToChange.add(addedScriptObj);
							}
						}
						logger.info(scriptsToChange.toString());
						this.changeScriptsPath(event.getProject().getId(), branchName, issueKey, scriptsToChange, destinationPath, currentScriptList);
					}
				}
			}
		}
	}

	private List<String> getAddedScripts(String projectId, BigDecimal mergeRequestIId) {
		GitlabMRChanges gitlabMRChanges = this.gitlabService.getMergeRequestChanges(projectId, mergeRequestIId);
		
		ArrayList<String> listScriptFiles = new ArrayList<>();

		if(gitlabMRChanges != null && !gitlabMRChanges.getChanges().isEmpty()) {
			for (GitlabChanges changes : gitlabMRChanges.getChanges()) {
				if(BooleanUtils.isTrue(changes.getNewFile())) {
					String addedFile = changes.getNewPath();
					if(addedFile.toLowerCase().endsWith(GitlabService.SCRIPT_EXTENSION) && !listScriptFiles.contains(addedFile)) {
						listScriptFiles.add(addedFile);
					}
				}
			}
		}

		logger.info("Scripts adicionados: {}", listScriptFiles.stream().collect(Collectors.joining(", ")));
		return listScriptFiles;
	}

	private void changeScriptsPath(BigDecimal projectId, String branchName, String issueKey, 
			List<GitlabScriptVersaoVO> scriptsToChange, String destinationPath, List<GitlabRepositoryTree> currentScriptList) {
		if(scriptsToChange != null && !scriptsToChange.isEmpty()) {
			// identify target version
			String[] pathArray = destinationPath.split("/");
			String targetVersion = pathArray[pathArray.length - 1];
			// get last current order
			Integer lastOrder = 0;
			for (GitlabRepositoryTree currentScript : currentScriptList) {
				GitlabScriptVersaoVO currentScriptObj = new GitlabScriptVersaoVO(currentScript.getPath());
				logger.info("Current script" + currentScriptObj.toString());
				if(currentScriptObj.getOrder() > lastOrder) {
					// can not count with scripts to change
					boolean hasToChange = false;
					for (GitlabScriptVersaoVO scriptToChange : scriptsToChange) {
						if(scriptToChange.getName().equals(currentScriptObj.getName())) {
							hasToChange = true;
							break;
						}
					}
					if(!hasToChange) {
						lastOrder = currentScriptObj.getOrder();
					}
				}
			}
			// sort scriptsToChange list
			Collections.sort(scriptsToChange, new SortbyOrder());
			for(int i=0; i < scriptsToChange.size(); i++) {
				GitlabScriptVersaoVO element = scriptsToChange.get(i);
				String numOrderStr = numOrderToString(++lastOrder);
				String newNameWithPath = destinationPath + "/" 
						+ "PJE_" + targetVersion + "_" + numOrderStr
						+ "__";
				if(element.getType() != null) {
					newNameWithPath += element.getType() + "_";
				}
				newNameWithPath += element.getSpecificName();
				element.setNewNameWithPath(newNameWithPath);
				scriptsToChange.set(i, element);
			}
			// encaminha para alteracao no gitlab
			String identificadorCommit = StringUtils.isNotBlank(issueKey) ? issueKey : "RELEASE";
			String commitMessage = identificadorCommit + " Reordenando arquivos de script";
			gitlabService.moveFiles(projectId, branchName, scriptsToChange, commitMessage);
		}
	}

	public String numOrderToString(Integer order) {
		String orderStr = order.toString();
		if(order < 10) {
			orderStr = "00" + orderStr;
		}else if(order < 100) {
			orderStr = "0" + orderStr;
		}

		return orderStr;
	}

	class SortbyOrder implements Comparator<GitlabScriptVersaoVO>{ 
		// Used for sorting in ascending order of 
		// order 
		public int compare(GitlabScriptVersaoVO a, GitlabScriptVersaoVO b) 
		{ 
			return a.getOrder() - b.getOrder(); 
		} 
	}
}


