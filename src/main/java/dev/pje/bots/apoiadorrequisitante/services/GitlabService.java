package dev.pje.bots.apoiadorrequisitante.services;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.devplatform.model.bot.VersionTypeEnum;
import com.devplatform.model.gitlab.GitLabPipeline;
import com.devplatform.model.gitlab.GitlabCommit;
import com.devplatform.model.gitlab.GitlabCommitAuthor;
import com.devplatform.model.gitlab.GitlabDiscussion;
import com.devplatform.model.gitlab.GitlabMergeRequestAttributes;
import com.devplatform.model.gitlab.GitlabMergeRequestPipeline;
import com.devplatform.model.gitlab.GitlabMergeRequestStateEnum;
import com.devplatform.model.gitlab.GitlabMergeRequestStatusEnum;
import com.devplatform.model.gitlab.GitlabNote;
import com.devplatform.model.gitlab.GitlabPipelineStatusEnum;
import com.devplatform.model.gitlab.GitlabProject;
import com.devplatform.model.gitlab.GitlabProjectExtended;
import com.devplatform.model.gitlab.GitlabProjectVariable;
import com.devplatform.model.gitlab.GitlabTag;
import com.devplatform.model.gitlab.GitlabTagRelease;
import com.devplatform.model.gitlab.GitlabUser;
import com.devplatform.model.gitlab.request.GitlabAcceptMRRequest;
import com.devplatform.model.gitlab.request.GitlabBranchRequest;
import com.devplatform.model.gitlab.request.GitlabCherryPickRequest;
import com.devplatform.model.gitlab.request.GitlabCommitActionRequest;
import com.devplatform.model.gitlab.request.GitlabCommitActionsEnum;
import com.devplatform.model.gitlab.request.GitlabCommitRequest;
import com.devplatform.model.gitlab.request.GitlabMRCommentRequest;
import com.devplatform.model.gitlab.request.GitlabMRRequest;
import com.devplatform.model.gitlab.request.GitlabMRUpdateRequest;
import com.devplatform.model.gitlab.request.GitlabRepositoryTagRequest;
import com.devplatform.model.gitlab.request.GitlabTagReleaseRequest;
import com.devplatform.model.gitlab.response.GitlabBranchResponse;
import com.devplatform.model.gitlab.response.GitlabCommitResponse;
import com.devplatform.model.gitlab.response.GitlabMRChanges;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.devplatform.model.gitlab.response.GitlabRefCompareResponse;
import com.devplatform.model.gitlab.response.GitlabRepositoryFile;
import com.devplatform.model.gitlab.response.GitlabRepositoryTree;
import com.devplatform.model.gitlab.vo.GitlabCommitFileVO;
import com.devplatform.model.gitlab.vo.GitlabMergeRequestVO;
import com.devplatform.model.gitlab.vo.GitlabScriptVersaoVO;
import com.fasterxml.jackson.core.JsonProcessingException;

import dev.pje.bots.apoiadorrequisitante.clients.GitlabClient;
import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.utils.GitlabUtils;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;

@Service
public class GitlabService {

	private static final Logger logger = LoggerFactory.getLogger(GitlabService.class);

	@Autowired
	private GitlabClient gitlabClient;
	
//	@Autowired
//	private TelegramService telegramService;
	
	@Autowired
	private SlackService slackService;

	@Autowired
	private RocketchatService rocketchatService;

	@Value("${clients.gitlab.url}")
	private String gitlabUrl;
	
	public static final String BRANCH_DEVELOP = "develop";
	public static final String BRANCH_MASTER = "master";
	public static final String BRANCH_RELEASE_CANDIDATE_PREFIX = "release-";
	public static final String TAG_RELEASE_CANDIDATE_SUFFIX = "-RC";
	
	public static final String PROJECT_DOCUMENTACAO = "276";
	public static final String PROJECT_PJE = "7";
	
	public static final String SCRIPS_MIGRATION_BASE_PATH = "pje-comum/src/main/resources/migrations/";
	public static final String SCRIPT_EXTENSION = ".sql";
	public static final String FIRST_SCRIPT_PREFIX = "PJE_";
	public static final String FIRST_SCRIPT_SUFFIX = "_001__VERSAO_INICIAL.sql";
	public static final String POMXML = "pom.xml";
	public static final String PACKAGE_JSON = "package.json";
	public static final String AUTHOR_NAME = "Bot Revisor do PJe";
	public static final String AUTHOR_EMAIL = "bot.revisor.pje@cnj.jus.br";
	
	public static final String LABEL_MR_LANCAMENTO_VERSAO = "Lancamento de versao";
	public static final String PREFIXO_LABEL_APROVACAO_TRIBUNAL = "Aprovado";
	
	public static final String GITLAB_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	
	public static final String JIRA_PROJECT_KEY = "JIRA_PROJECT_KEY";
	public static final String PROJECT_PROPERTY_POM_VERSION_TAGNAME = "POM_TAGNAME_PROJECT_VERSION";
	
	public static final String POM_TAGNAME_PROJECT_VERSION_DEFAULT = "project/version";
	
	public List<GitlabRepositoryTree> getFilesFromPath(BigDecimal projectId, String branch, String path) {
		return getFilesFromPath(projectId.toString(), branch, path);
	}
	public List<GitlabRepositoryTree> getFilesFromPath(String projectId, String branch, String path) {
		List<GitlabRepositoryTree> listFiles = new ArrayList<>();
		try {
			List<GitlabRepositoryTree> listElements = gitlabClient.getRepositoryTree(projectId, path, branch);
			for (GitlabRepositoryTree element : listElements) {
				if(!element.getType().equals("tree")) {
					listFiles.add(element);
				}
			}
		}catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}
		
		return listFiles;
	}

	public GitlabCommitResponse moveFiles(BigDecimal projectId, String branch, List<GitlabScriptVersaoVO> scriptsToChange, String commitMessage) {
		return moveFiles(projectId.toString(), branch, scriptsToChange, commitMessage);
	}
	
	public GitlabCommitResponse moveFiles(String projectId, String branch, List<GitlabScriptVersaoVO> scriptsToChange, String commitMessage) {

		GitlabCommitResponse response = null;
		if(scriptsToChange != null && !scriptsToChange.isEmpty()) {
			GitlabCommitRequest commit = new GitlabCommitRequest();
			commit.setBranch(branch);
			commit.commitMessage(commitMessage);
			
			List<GitlabCommitActionRequest> actions = new ArrayList<>();
			for (GitlabScriptVersaoVO scriptToChange : scriptsToChange) {
				GitlabCommitActionRequest action = new GitlabCommitActionRequest();
				action.setAction(GitlabCommitActionsEnum.MOVE);
				action.setPreviousPath(scriptToChange.getNameWithPath());
				action.setFilePath(scriptToChange.getNewNameWithPath());
				
				actions.add(action);
			}
			commit.setActions(actions);
			
			logger.info(commitMessage);

			try {
				response = gitlabClient.sendCommit(projectId, commit);
				if(response != null && response.getId() != null) {
					logger.info("ok");
				}
			}catch(Exception e) {
				logger.error("N??o foi poss??vel mover os arquivos", e);
			}
		}
		return response;
	}
	
	public String getScriptsMigrationBasePath(String projectId) {
		String scriptsMigrationBasePath = null;
		if(StringUtils.isNotBlank(projectId)) {
			if(projectId.equals(PROJECT_PJE)) {
				scriptsMigrationBasePath = GitlabService.SCRIPS_MIGRATION_BASE_PATH;
			}
		}
		return scriptsMigrationBasePath;
	}
	
	public GitlabCommitResponse createScriptsDir(String projectId, String branchName, String lastCommitId, String version, String commitMessage) {
		GitlabCommitResponse response = null;
		// monta o path dos scripts
		String scriptsMigrationBasePath = getScriptsMigrationBasePath(projectId);
		if(StringUtils.isNotBlank(scriptsMigrationBasePath)) {
			String destinationPath = scriptsMigrationBasePath + version;
			List<GitlabRepositoryTree> currentScriptList = getFilesFromPath(projectId, branchName, destinationPath);
			// verifica se a pasta ainda n??o existe
			if(currentScriptList == null || currentScriptList.isEmpty()) {
				// cria a pasta com o novo arquivo
				GitlabCommitRequest commit = new GitlabCommitRequest();
				commit.setBranch(branchName);
				commit.commitMessage(commitMessage);
				commit.setAuthorName(AUTHOR_NAME);
				commit.setAuthorEmail(AUTHOR_EMAIL);
				
				List<GitlabCommitActionRequest> actions = new ArrayList<>();
				
				String firstScriptPath = destinationPath + "/" + FIRST_SCRIPT_PREFIX + version + FIRST_SCRIPT_SUFFIX;
				
				GitlabCommitActionRequest action = new GitlabCommitActionRequest();
				action.setAction(GitlabCommitActionsEnum.CREATE);
				action.setFilePath(firstScriptPath);
				action.setContent("-- Arquivo inicial dos scripts da versao " + version);
				action.setLastCommitId(lastCommitId);
				actions.add(action);
				
				commit.setActions(actions);
				
				logger.info(commitMessage);
//				telegramService.sendBotMessage(commitMessage);
				
				try {
					response = gitlabClient.sendCommit(projectId, commit);
					if(response != null && response.getId() != null) {
						logger.info("ok");
					}
				}catch(Exception e) {
					String errorMessage = "N??o foi poss??vel mover os arquivos do commit: " + lastCommitId + "\n"
							+e.getMessage();
					logger.error(errorMessage);
					slackService.sendBotMessage(errorMessage);
					rocketchatService.sendBotMessage(errorMessage);
//					telegramService.sendBotMessage(errorMessage);
				}
			}else {
				String message = "J?? existe a pasta de scripts da vers??o: " + version + "";
				logger.info(message);
				slackService.sendBotMessage(message);
				rocketchatService.sendBotMessage(message);
//				telegramService.sendBotMessage(message);
				
			}
		}else {
			String message = "N??o h?? pasta de sciprts para o projeto: " + projectId + "";
			logger.info(message);
			slackService.sendBotMessage(message);
			rocketchatService.sendBotMessage(message);
//			telegramService.sendBotMessage(message);
		}
		return response;
	}
	
	public GitlabCommitResponse renameDir(String projectId, String branchName,
			String previousPath, String newPath, String commitMessage, String currentVersion, String newVersion) {
		
		GitlabCommitResponse response = null;
		if(StringUtils.isNotBlank(previousPath) && StringUtils.isNotBlank(newPath) && !previousPath.equals(newPath)) {
			// busca todos os arquivos de uma determinada pasta previouspath
			List<GitlabRepositoryTree> currentScriptList = getFilesFromPath(projectId, branchName, previousPath);
			// move esses arquivos para a pasta destino newpath

			GitlabCommitRequest commit = new GitlabCommitRequest();
			commit.setBranch(branchName);
			commit.commitMessage(commitMessage);
			commit.setAuthorName(AUTHOR_NAME);
			commit.setAuthorEmail(AUTHOR_EMAIL);
			
			List<GitlabCommitActionRequest> actions = new ArrayList<>();

			if(currentScriptList != null && !currentScriptList.isEmpty()) {
				for (GitlabRepositoryTree currentScript : currentScriptList) {
					String currentFilePath = currentScript.getPath();
					String newFilePath = newPath + "/" + this.renameFile(currentScript.getName(), currentVersion, newVersion);
					
					GitlabCommitActionRequest action = new GitlabCommitActionRequest();
					action.setAction(GitlabCommitActionsEnum.MOVE);
					action.setPreviousPath(currentFilePath);
					action.setFilePath(newFilePath);
//					action.setLastCommitId(lastCommitId);
					
					actions.add(action);
				}
				// the folder will be automatically deleted once it is an empty folder
				commit.setActions(actions);
				
				
				try {
					logger.info(commitMessage);
//					telegramService.sendBotMessage(commitMessage);
					response = gitlabClient.sendCommit(projectId, commit);
					if(response != null && response.getId() != null) {
						logger.info("ok");
					}
				}catch(Exception e) {
					String errorMessage = "N??o foi poss??vel renomear o diretorio: " + previousPath + " para: " + newPath + "\n"
							+e.getMessage();
					logger.error(errorMessage);
					slackService.sendBotMessage(errorMessage);
					rocketchatService.sendBotMessage(errorMessage);
//					telegramService.sendBotMessage(errorMessage);
				}
			}
		}
		return response;
	}
	
	private String renameFile(String fileName, String currentVersion, String newVersion) {
		return fileName.replaceFirst(currentVersion, newVersion);
	}
	
	public GitlabCommitResponse sendTextAsFileToBranch(String projectId, GitlabBranchResponse branch, String filePath, String content, String commitMessage) {
		String branchName = branch.getBranchName();
		return sendTextAsFileToBranch(projectId, branchName, filePath, content, commitMessage);
	}
	
	public GitlabCommitResponse sendTextAsFileToBranch(String projectId, String branchName, String filePath, String content, String commitMessage) {
		return sendTextAsFileToBranch(projectId, branchName, Collections.singletonMap(filePath, content), commitMessage);
	}
	
	public GitlabCommitResponse sendTextAsFileToBranch(String projectId, String branchName, Map<String, String> mapFiles, String commitMessage) {
		List<GitlabCommitFileVO> files = new ArrayList<>();
		if(mapFiles != null && mapFiles.size() > 0) {
			for (Map.Entry<String, String> mapFile : mapFiles.entrySet()) {
				String filePath = mapFile.getKey();
				String content = mapFile.getValue();
				
				GitlabCommitFileVO file = new GitlabCommitFileVO(filePath, content, false);
				files.add(file);
			}
		}
		return sendFilesToBranch(projectId, branchName, files, commitMessage);
	}
	
	public GitlabCommitResponse sendFilesToBranch(String projectId, String branchName, List<GitlabCommitFileVO> files, String commitMessage) {
		GitlabCommitResponse response = null;
		if(files != null && files.size() > 0) {
			GitlabCommitRequest commit = new GitlabCommitRequest();
			commit.setBranch(branchName);
			StringBuilder sb = new StringBuilder();
			if(commitMessage != null){
				sb.append(commitMessage);
			}else{
				sb
				.append("[")
				.append(branchName)
				.append("] ")
				.append("Adicionando arquivos");
			}
			commit.setCommitMessage(sb.toString());
			commit.setAuthorName(AUTHOR_NAME);
			commit.setAuthorEmail(AUTHOR_EMAIL);
			
			List<GitlabCommitActionRequest> actionRequestList = new ArrayList<>();
			if(files != null && files.size() > 0) {
				for (GitlabCommitFileVO file : files) {
					String filePath = file.getPath();
					String content = file.getContent();
					boolean isBase64 = file.getBase64();
					
					GitlabCommitActionRequest actionRequest = new GitlabCommitActionRequest();
					GitlabCommitActionsEnum commitAction = GitlabCommitActionsEnum.CREATE;
					// verifica se o arquivo j?? existe, se j?? existir o substitui
					GitlabRepositoryFile releaseFile = getFile(projectId, filePath, branchName);
					if(releaseFile != null){
						commitAction = GitlabCommitActionsEnum.UPDATE;
					}
					actionRequest.setAction(commitAction);
					
					if(!isBase64 && StringUtils.isNotBlank(content)) {
						content = Utils.textToBase64(content);
						isBase64 = true;
					}
					
					actionRequest.setContent(content);
					actionRequest.setFilePath(filePath);
					if(isBase64) {
						actionRequest.setEncoding("base64");
					}else {
						actionRequest.setEncoding("text");
					}
					actionRequestList.add(actionRequest);
				}
			}
			commit.setActions(actionRequestList);
			logger.info("commit string: " + Utils.convertObjectToJson(commit));
			response = sendCommit(projectId, commit);
		}
		return response;
	}
	
	public GitlabCommitResponse sendCommit(String projectId, GitlabCommitRequest commit) {
		GitlabCommitResponse response = null;
		try {
			response = gitlabClient.sendCommit(projectId, commit);	
		}catch (Exception e) {
			String errorMessage = "[GITLAB] - Project: " + projectId + " - error trying to send commit [" + commit.getCommitMessage() + "]: \n"
					+e.getMessage();
			logger.error(errorMessage);
			slackService.sendBotMessage(errorMessage);
			rocketchatService.sendBotMessage(errorMessage);
//			telegramService.sendBotMessage(errorMessage);
		}
		
		return response;
	}
	
	public GitlabRepositoryFile getFile(String projectId, String filePath, String ref){
		GitlabRepositoryFile file = null;
		try{
			String filePathEncoded = Utils.urlEncode(filePath);
			file = gitlabClient.getFile(projectId, filePathEncoded, ref);
		}catch (Exception e) {
			if(e instanceof UnsupportedEncodingException){
				logger.error("Filepath could not be used: " + filePath + " - error: " + e.getLocalizedMessage());
			}else{
				logger.error("File not found " + e.getLocalizedMessage());
			}
		}
		return file;
	}
	
	public String getDefaultBranch(String projectId) {
		String branchDefault = BRANCH_MASTER;

		GitlabProjectExtended project = this.getProjectDetails(projectId);
		if (project != null && StringUtils.isNotBlank(project.getDefaultBranch())) {
			branchDefault = project.getDefaultBranch();
		}

		return branchDefault;
	}
	
	public String getRawFileFromDefaultBranch(String projectId, String filePath){
		String branchDefault = getDefaultBranch(projectId);
		return getRawFile(projectId, branchDefault, filePath);
	}
	
	public String getRawFile(String projectId, String branchName, String file) {
		String fileRawContent = null;

		try {
			fileRawContent = gitlabClient.getRawFile(projectId, Utils.urlEncode(file), branchName);
		} catch (Exception e) {
			logger.error("Falha ao recuperar arquivo {} do branch {} do projeto {}. Erro: {}", file, branchName, projectId, e.getLocalizedMessage());
		}

		return fileRawContent;
	}
	
	public void cherryPick(GitlabProject project, String branchName, List<GitlabCommit> commits) {
		if(project != null && (branchName != null && !branchName.isEmpty()) && commits != null && !commits.isEmpty()) {
			String projectName = project.getName();
			String projectId = project.getId().toString();
			for (GitlabCommit commit : commits) {
				String commitSHA = commit.getId();
				
				// TODO - verifica se o commit j?? existe no target branch
				
				GitlabCherryPickRequest cherryPick = new GitlabCherryPickRequest();
				cherryPick.setId(projectId);
				cherryPick.setBranch(branchName);
				cherryPick.setSha(commitSHA);

				String messageToCherryPick = "[GITFLOW][GITLAB] - Project: " + projectName + " - applying commit [" + commitSHA + "] into branch:" + branchName;
				logger.info(messageToCherryPick);
//				telegramService.sendBotMessage(messageToCherryPick);

				try {
					GitlabCommitResponse response = gitlabClient.cherryPick(projectId, commitSHA, cherryPick);
					if(response != null && response.getId() != null) {
						logger.info("ok");
					}
				}catch(Exception e) {
					String errorMessage = "[GITFLOW][GITLAB] - Project: " + projectName + " - error trying to apply commit [" + commitSHA + "] into branch "+ branchName +": \n"
							+e.getMessage();
					logger.error(errorMessage);
					slackService.sendBotMessage(errorMessage);
					rocketchatService.sendBotMessage(errorMessage);
//					telegramService.sendBotMessage(errorMessage);
				}
			}
		}
	}
	
	public GitlabTag getVersionTag(String projectId, String version) {
		GitlabTag tag = null;
		if(StringUtils.isNotBlank(version) && StringUtils.isNotBlank(projectId)) {
			try {
				tag = gitlabClient.getSingleRepositoryTag(projectId, version);
			}catch (Exception e) {
				logger.error(e.getLocalizedMessage());
			}
		}
		return tag;
	}
	
	public GitlabTag createVersionTag(String projectId, String tagName, String branchName, String tagMessage) {
		GitlabTag tag = null;

		if (StringUtils.isNotBlank(tagName) && StringUtils.isNotBlank(projectId)) {
			GitlabRepositoryTagRequest tagRequest = new GitlabRepositoryTagRequest();
			tagRequest.setTagName(tagName);
			tagRequest.setRef(branchName);
			tagRequest.setMessage(tagMessage);

			try {
				tag = this.gitlabClient.createRepositoryTag(projectId, tagRequest);
			} catch (Exception e) {
				logger.error("Falha ao criar a tag {} para o projeto {}. Erro: {}", tagName, projectId, e.getLocalizedMessage());
			}
		}

		return tag;
	}
	
	public GitlabTagRelease createTagRelease(String projectId, String tagName, String releaseText) {
		GitlabTagRelease tagRelease = null;
		if(StringUtils.isNotBlank(tagName) && StringUtils.isNotBlank(projectId)) {
			try {
				GitlabTagReleaseRequest tagReleaseRequest = new GitlabTagReleaseRequest(tagName, releaseText);
				tagRelease = gitlabClient.createSimpleTagRelease(projectId, tagReleaseRequest);
			}catch (Exception e) {
				logger.error(e.getLocalizedMessage());
			}
		}
		return tagRelease;
		
	}

	public boolean isDevelopDefaultBranch(GitlabProject project) {
		boolean isDevelopDefaultBranch = false;
		if(project != null) {
			if(StringUtils.isNotBlank(project.getDefaultBranch())) {
				isDevelopDefaultBranch = BRANCH_DEVELOP.equals(project.getDefaultBranch());
			}
		}
		return isDevelopDefaultBranch;
	}
	
	public boolean isMonitoredBranch(String branchName) {
		return BRANCH_DEVELOP.equals(branchName) || isBranchRelease(branchName) || isBranchMaster(branchName);
	}
	
	public boolean isBranchMaster(String branchName) {
		return BRANCH_MASTER.equals(branchName);
	}
	
	public boolean isBranchRelease(String branchName) {
		return branchName.toLowerCase().startsWith(BRANCH_RELEASE_CANDIDATE_PREFIX);
	}
	
	public GitlabBranchResponse createBranch(String projectId, String branchName, String branchRef) {
		GitlabBranchRequest branch = new GitlabBranchRequest();

		branch.setBranch(branchName);
		branch.setRef(branchRef);
		
		GitlabBranchResponse response = null;
		try {
			response = getSingleRepositoryBranch(projectId, branchName);
			if(response == null) {
				response = gitlabClient.createRepositoryBranch(projectId, branch);
			}
		}catch (Exception e) {
			String errorMessage = "Erro ao tentar criar o branch: " + branchName 
					+ "no projeto: " + projectId+": \n"
					+e.getMessage();
			logger.error(errorMessage);
			slackService.sendBotMessage(errorMessage);
			rocketchatService.sendBotMessage(errorMessage);
//			telegramService.sendBotMessage(errorMessage);
		}
		
		return response;
	}
	
	public GitlabBranchResponse getSingleRepositoryBranch(String projectId, String branchName) {
		GitlabBranchResponse branch = null;

		try {
			branch = gitlabClient.getSingleRepositoryBranch(projectId, branchName);
		} catch (Exception e) {
			logger.error("Falha ao recuperar o branch {} no projeto {}. Erro: {}", branchName, projectId, e.getLocalizedMessage());
		}

		return branch;
	}
	
	public GitlabBranchResponse createFeatureBranch(String projectId, String branchName) {
		GitlabBranchResponse featureBranch = getSingleRepositoryBranch(projectId, branchName);

		if (featureBranch == null) {
			if (isProjectImplementsGitflow(projectId)) {
				featureBranch = createBranch(projectId, branchName, BRANCH_DEVELOP);
			} else {
				featureBranch = createBranch(projectId, branchName, BRANCH_MASTER);
			}
		}

		return featureBranch;
	}
	
	public String getActualReleaseBranch(GitlabProject project) {
		return getActualReleaseBranch(project.getId().toString());
	}
	
	public String getActualReleaseBranch(String projectId) {
		String actualReleaseBranch = null;
		List<GitlabBranchResponse> branches = gitlabClient.searchBranches(projectId, BRANCH_RELEASE_CANDIDATE_PREFIX);
		if(branches != null && !branches.isEmpty()) {
			GitlabBranchResponse lastBranch = null;
			List<Integer> lastVersionNumbers = null;;
			for (GitlabBranchResponse branch : branches) {
				String versionStr = branch.getBranchName().replace(BRANCH_RELEASE_CANDIDATE_PREFIX, "");
				List<Integer> versionNumbers = Utils.getVersionFromString(versionStr);
				if(versionNumbers != null && !versionNumbers.isEmpty() && !branch.getMerged()) {
					if(lastBranch == null) {
						lastVersionNumbers = versionNumbers;
						lastBranch = branch;
					}else {
						int diff = 0;
				    	if(versionNumbers != null && lastVersionNumbers != null) {
				    		if(versionNumbers.size() >= lastVersionNumbers.size()) {
				    			diff = Utils.compareVersionsDesc(lastVersionNumbers, versionNumbers);
				    		}else {
				    			diff = (-1) * Utils.compareVersionsDesc(lastVersionNumbers, versionNumbers);
				    		}
				    	}
				    	if(diff > 0) {
							lastVersionNumbers = versionNumbers;
							lastBranch = branch;
				    	}
					}
				}
			}
			if(lastBranch != null) {
				actualReleaseBranch = lastBranch.getBranchName();
			}
		}
		return actualReleaseBranch;
	}
	
	@Cacheable(cacheNames = "project-gitflow")
	public boolean isProjectImplementsGitflow(String projectId) {
		Boolean implementsGitflow = false;
		String defaultBranch = getDefaultBranch(projectId);
		implementsGitflow = BRANCH_DEVELOP.equalsIgnoreCase(defaultBranch);
		return implementsGitflow;
	}
	
	public GitlabProjectExtended getProjectDetails(String projectId) {
		GitlabProjectExtended project = null;

		try {
			project = gitlabClient.getSingleProject(projectId);
		} catch (Exception e) {
			logger.error("Falha ao recuperar informa????es sobre o projeto {}. Erro: {}", projectId, e.getLocalizedMessage());
		}

		return project;
	}

	@Cacheable(cacheNames = "search-projects")
	public List<GitlabProjectExtended> searchProjectByNamespace(String projectNamespace) {
		List<GitlabProjectExtended> projects = null;
		Map<String, String> searchData = new HashMap<>();
		try {
			searchData.put("search_namespaces", Boolean.TRUE.toString());
			searchData.put("search", projectNamespace);
			projects = gitlabClient.searchProject(searchData);
		}catch (Exception e) {
			String msgError = "Falhou ao tentar buscar o projeto: " + projectNamespace + "  - erro: " + e.getLocalizedMessage();
			logger.error(msgError);
//			telegramService.sendBotMessage(msgError);
		}
		return projects;
	}
	
	public boolean isBranchMergedIntoTarget(String projectId, String branchSource, String branchTarget) {
		boolean branchMerged = false;

		try {
			GitlabRefCompareResponse compareResponse = gitlabClient.compareBranches(projectId, branchTarget, branchSource);
			branchMerged = compareResponse != null && compareResponse.getDiffs() != null && compareResponse.getDiffs().isEmpty();
		} catch (Exception e) {
			logger.error("Falha ao comparar os branches {} (source) e {} (target) do projeto {}. Erro: {}", branchSource, branchTarget, projectId, e.getLocalizedMessage());
		}

		return branchMerged;
	}

	public void closeMergeRequestThatCannotBeMerged(String projectId) {
		Map<String, String> options = new HashMap<>();
		options.put("state", "opened");
		options.put("order_by", "updated_at");
		options.put("sort", "asc");

		boolean hasNext = true;
		for (int i = 1; hasNext; i++) {
			options.put("page", Integer.toString(i));

			List<GitlabMRResponse> mergeRequests = this.findMergeRequests(projectId, options);

			if (mergeRequests.isEmpty()) {
				hasNext = false;
				continue;
			}

			for (GitlabMRResponse mergeRequest : mergeRequests) {
				mergeRequest = getMergeRequest(projectId, mergeRequest.getIid());

				while (true) {
					if (GitlabMergeRequestStatusEnum.CHECKING.equals(mergeRequest.getMergeStatus()) || GitlabMergeRequestStatusEnum.CANNOT_BE_MERGED_RECHECK.equals(mergeRequest.getMergeStatus())) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						break;
					}
					
					mergeRequest = getMergeRequest(projectId, mergeRequest.getIid());
				}

				logger.info("MR# " + mergeRequest.getIid());
				logger.info("Title: " + mergeRequest.getTitle());
				logger.info("merge_status: " + mergeRequest.getMergeStatus().name());
				logger.info("has_conflicts: " + mergeRequest.getHasConflicts());

				if (GitlabMergeRequestStatusEnum.CAN_NOT_BE_MERGED.equals(mergeRequest.getMergeStatus()) || mergeRequest.getHasConflicts()) {
					this.sendMergeRequestComment(projectId, mergeRequest.getIid(), String.format(
							"O c??digo enviado apresenta conflitos de integra????o. Favor realizar rebase com o branch %s", mergeRequest.getTargetBranch()));

					this.closeMergeRequest(projectId, mergeRequest.getIid());
				}
			}
		}
		
	}

	public List<GitlabMRResponse> findMergeRequests(String projectId, Map<String, String> options) {
		List<GitlabMRResponse> mergeRequests = new ArrayList<>();

		try {
			mergeRequests = gitlabClient.findMergeRequest(projectId, options);
		} catch (Exception e) {
			logger.error("Falha ao pesquisar MRs para o projeto " + projectId, e);
		}

		return mergeRequests;
	}

	public List<GitlabMRResponse> findMergeRequestAllProjects(Map<String, String> options){
		List<GitlabMRResponse> MRs = new ArrayList<>();
		try {
			if(options == null) {
				options = new HashMap<>();
			}
			
			Integer page = 1;
			boolean finalizado = false;
			while(!finalizado) {
				options.put("page", page.toString());
				List<GitlabMRResponse> pageMRs = gitlabClient.findMergeRequestAllProjects(options);
				if(pageMRs != null && !pageMRs.isEmpty()) {
					MRs.addAll(pageMRs);
					page++;
				}else {
					finalizado = true;
				}
			}
		}catch (Exception e) {
			String msgError = "Erro ao buscar MRs: erro: " + e.getLocalizedMessage();
			logger.error(msgError);
//			telegramService.sendBotMessage(msgError);
		}
		return MRs;
	}
	
	public List<GitlabMRResponse> findAllOpenMergeRequestsFromIssueKey(String issueKey){
		Map<String, String> options = new HashMap<String, String>();
		options.put("state", GitlabMergeRequestStateEnum.OPENED.toString());
		options.put("search", issueKey);
		options.put("in", "title");
		
		return findMergeRequestAllProjects(options);
	}

	public GitlabMRResponse getMergeRequest(String projectId, BigDecimal mergeRequestIId) {
		GitlabMRResponse gitlabMRResponse = null;

		try {
			gitlabMRResponse = gitlabClient.getSingleMergeRequest(projectId, mergeRequestIId);
		} catch (Exception e) {
			logger.error("Falha ao recuperar MR#{} do projeto {}. Erro: {}", mergeRequestIId, projectId, e.getLocalizedMessage());
		}

		return gitlabMRResponse;
	}

	public GitlabMRChanges getMergeRequestChanges(String projectId, BigDecimal mergeRequestIId) {
		GitlabMRChanges gitlabMRChanges = null;

		try {
			gitlabMRChanges = gitlabClient.getSingleMergeRequestChanges(projectId, mergeRequestIId);
		} catch (Exception e) {
			logger.error("Falha ao recuperar as mudan??as do MR#{} do projeto {}. Erro: {}", mergeRequestIId, projectId, e.getLocalizedMessage());
		}

		return gitlabMRChanges;
	}
	
	public List<GitlabCommitResponse> getCommits(BigDecimal gitlabProjectId, BigDecimal mergeRequestId) {
		List<GitlabCommitResponse> commits = new ArrayList<>();
		try {
			commits = gitlabClient.getSingleMergeRequestCommits(gitlabProjectId, mergeRequestId);
		} catch (Exception e) {
			logger.error("Erro ao recuperar os commits do MR#" + mergeRequestId.toString() + " do projeto " + gitlabProjectId, e);
		}
		return commits;
	}

	public GitlabMRResponse rebaseMergeRequest(String projectId, BigDecimal mergeRequestIId) throws GitlabException {
		GitlabMRResponse gitlabMRResponse = null;
		try {
			logger.info("Realizando rebase do MR#{}", mergeRequestIId);
			gitlabMRResponse = gitlabClient.rebaseMergeRequest(projectId, mergeRequestIId);
			if (BooleanUtils.isTrue(gitlabMRResponse.getRebaseInProgress())) {
				while (true) {
					logger.info("Em processamento rebase do MR#{}. Thread.sleep(5000)", mergeRequestIId);
					Thread.sleep(5000);
					gitlabMRResponse = gitlabClient.getSingleMergeRequest(projectId, mergeRequestIId);
					if (BooleanUtils.isFalse(gitlabMRResponse.getRebaseInProgress())) {
						if (StringUtils.isNotEmpty(gitlabMRResponse.getMergeError())) {
							logger.info("Rebase do MR#{} finalizado com erro (1).", mergeRequestIId);
							throw new Exception(gitlabMRResponse.getMergeError());
						}
						logger.info("Rebase do MR#{} finalizado.", mergeRequestIId);
						break;
					}
				}
			} else {
				if (StringUtils.isNotEmpty(gitlabMRResponse.getMergeError())) {
					logger.info("Rebase do MR#{} finalizado com erro (2).", mergeRequestIId);
					throw new Exception(gitlabMRResponse.getMergeError());
				}
			}
//			logger.info("hold 1 second before return rebaseMergeRequest(String {}, BigDecimal {})", projectId, mergeRequestIId);
//			Thread.sleep(1000);
		} catch (Exception e) {
			throw new GitlabException(String.format("Falha ao realizar rebase do MR#%s do projeto %s. Erro: %s", mergeRequestIId, projectId, e.getLocalizedMessage()));
		}
		return gitlabMRResponse;
	}

	public String checkMRsOpened(String mergesWebUrls) {
		List<String> mrAbertosConfirmados = new ArrayList<>();
		
		List<GitlabMergeRequestVO> MRsVO = GitlabUtils.getMergeRequestVOListFromString(mergesWebUrls, gitlabUrl);
		// pesquisar MR com a identificacao do projeto + n??mero do MR
		if(MRsVO != null && !MRsVO.isEmpty()) {
			Map<String, GitlabProjectExtended> projectsCache = new HashMap<>();
			for (GitlabMergeRequestVO MR : MRsVO) {
				String projectNamespace = MR.getProjectNamespace();
				if(StringUtils.isNotBlank(projectNamespace)) {
					if(projectsCache.get(projectNamespace) == null) {
						List<GitlabProjectExtended> projects = searchProjectByNamespace(projectNamespace);
						if(projects != null && !projects.isEmpty()) {
							for (GitlabProjectExtended project : projects) {
								String pathWithNamespace = project.getPathWithNamespace();
								projectsCache.put(pathWithNamespace, project);
							}
						}
					}
					if(projectsCache.get(projectNamespace) != null) {
						GitlabProjectExtended project = projectsCache.get(projectNamespace);
						BigDecimal mrIID = MR.getMrIId();
						// pesquisa pelo MR
						if(project != null && project.getId() != null && mrIID != null) {
							GitlabMRResponse response = getMergeRequest(project.getId().toString(), mrIID);
							if(response != null && GitlabMergeRequestStateEnum.OPENED.equals(response.getState())) {
								if(StringUtils.isNotBlank(response.getWebUrl()) && !mrAbertosConfirmados.contains(response.getWebUrl())) {
									mrAbertosConfirmados.add(response.getWebUrl());
								}
							}
						}
					}
				}
			}	
		}
		return String.join(", ", mrAbertosConfirmados);
	}

	/**
	 * 
	 * @param projectId
	 * @param branchReleaseName
	 * @param commitMessage
	 * @throws GitlabException 
	 * @throws Exception 
	 */
	public GitlabMRResponse mergeBranchReleaseIntoMaster(String projectId, String sourceBranch, String mergeMessage) throws GitlabException {		
		return mergeSourceBranchIntoBranchTarget(projectId, sourceBranch, BRANCH_MASTER, mergeMessage, LABEL_MR_LANCAMENTO_VERSAO, Boolean.FALSE, Boolean.TRUE);
	}

	public GitlabMRResponse mergeFeatureBranchIntoBranchDefault(String projectId, String featureBranch, String mergeMessage) throws Exception {
		String defaultBranch = getDefaultBranch(projectId);
		Boolean squashCommits = true;
		Boolean removeSourceBranch = true;

		return mergeSourceBranchIntoBranchTarget(projectId, featureBranch, defaultBranch, mergeMessage, null, squashCommits, removeSourceBranch);
	}

	public GitlabMRResponse openMergeRequestIntoBranchDefault(String projectId, String featureBranch, String mergeMessage) {
		String defaultBranch = getDefaultBranch(projectId);
		Boolean squashCommits = true;
		Boolean removeSourceBranch = true;

		return openMergeRequest(projectId, featureBranch, defaultBranch, mergeMessage, null, squashCommits, removeSourceBranch);
	}

	/**
	 * Pesquisa para saber se o MR j?? foi pedido
	 * - se n??o, abre o MR
	 * Com o MR, verifica se o MR possui algum pipeline
	 * - se n??o, marca: mergeWhenPipelineSucceeds = false
	 * - se sim, marca: mergeWhenPipelineSucceeds = true
	 * Aceita o MR
	 * 
	 * @param projectId
	 * @param sourceBranch
	 * @param targetBranch
	 * @param commitMessage
	 * @param labels
	 * @param squashCommits
	 * @param removeSourceBranch
	 * @return
	 * @throws GitlabException 
	 * @throws Exception
	 */
	public GitlabMRResponse mergeSourceBranchIntoBranchTarget(String projectId, String sourceBranch, String targetBranch, 
			String mergeMessage, String labels, Boolean squashCommits, Boolean removeSourceBranch) throws GitlabException {

		GitlabMRResponse mrAccepted = null;

		GitlabMRResponse mrOpened = this.openMergeRequest(projectId, sourceBranch, targetBranch, mergeMessage, labels, squashCommits, removeSourceBranch);
		if(mrOpened != null) {
			mrAccepted = this.acceptMergeRequest(projectId, mrOpened.getIid(), squashCommits, removeSourceBranch);
			this.deleteBranch(projectId, sourceBranch);
		}

		return mrAccepted;
	}

	public void deleteBranch(String projectId, String branch) {
		try {
			this.gitlabClient.deleteRepositoryBranch(projectId, branch);
		} catch (Exception e) {
			logger.error("Falha ao excluir branch {} do projeto {}. Erro: {}", branch, projectId, e.getLocalizedMessage());
		}
	}
	
	public GitlabMRResponse openMergeRequest(String projectId, String sourceBranch, String targetBranch,
			String mergeMessage, String labels, boolean squashCommits, boolean removeSourceBranch) {
		
		GitlabMRResponse mrOpened = null;
		boolean releaseBranchMerged = this.isBranchMergedIntoTarget(projectId, sourceBranch, targetBranch);

		if (!releaseBranchMerged) {
			Map<String, String> options = new HashMap<>();
			options.put("state", GitlabMergeRequestStateEnum.OPENED.toString());
			options.put("source_branch", sourceBranch);
			options.put("target_branch", targetBranch);

			List<GitlabMRResponse> mergeRequests = this.findMergeRequests(projectId, options);
			if (!mergeRequests.isEmpty()) {
				mrOpened = mergeRequests.get(0);
			} else {
				GitlabMRRequest mergeRequest = new GitlabMRRequest();
				mergeRequest.setSourceBranch(sourceBranch);
				mergeRequest.targetBranch(targetBranch);
				mergeRequest.setLabels(labels);
				mergeRequest.title(mergeMessage);
				mergeRequest.setSquash(squashCommits);
				mergeRequest.setRemoveSourceBranch(removeSourceBranch);

				mrOpened = this.openMergeRequest(projectId, mergeRequest);
			}
		} else {
			logger.info("O branch {} j?? foi integrado ao branch {} para o projeto {} ", sourceBranch, targetBranch, projectId);
		}

		return mrOpened;
	}
	
	public GitlabMRResponse closeMergeRequest(String projectId, BigDecimal mergeRequestIId) {
		GitlabMRUpdateRequest updateMerge = new GitlabMRUpdateRequest();
		updateMerge.setMergeRequestIid(mergeRequestIId);
		updateMerge.setStateEvent("close");

		return updateMergeRequest(projectId, mergeRequestIId, updateMerge);
	}
	
	public GitlabMRResponse acceptMergeRequest(String projectId, BigDecimal mrIID, Boolean squashCommits, Boolean removeSourceBranch) throws GitlabException {
		GitlabMRResponse mrOpened = getMergeRequest(projectId, mrIID);
		if (mrOpened != null) {
			if (!mrOpened.getState().equals(GitlabMergeRequestStateEnum.OPENED)) {
				throw new GitlabException(String.format("MR#%s n??o pode ser aceito pois possui status igual a %s", mrOpened.getIid(), mrOpened.getState()));
//			} else if (BooleanUtils.isTrue(mrOpened.getHasConflicts())) {
//				throw new GitlabException(String.format("MR#%s n??o pode ser aceito pois apresenta conflitos de integra????o", mrOpened.getIid()));
			} else if (mrOpened.getHeadPipeline() != null && mrOpened.getHeadPipeline().getStatus().equals(GitlabPipelineStatusEnum.FAILED)) {
				throw new GitlabException(String.format("MR#%s n??o pode ser aceito pois o processamento do CI/CD n??o encerrou corretamente", mrOpened.getIid()));
			} else {
				logger.info("MR#{} com status {}", mrIID, mrOpened.getMergeStatus().name());

				if (!mrOpened.getMergeStatus().equals(GitlabMergeRequestStatusEnum.CAN_BE_MERGED)) {
					do {
						try {
							logger.info("Waiting for 5 seconds");
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						logger.info("Poll the API endpoint \"getMergeRequest({}, {})\" to get updated status.", projectId, mrIID);
						mrOpened = getMergeRequest(projectId, mrIID);
						logger.info("MR#{} com status {}", mrIID, mrOpened.getMergeStatus().name());
					} while (mrOpened.getMergeStatus().equals(GitlabMergeRequestStatusEnum.CHECKING));
				}

				if (BooleanUtils.isTrue(mrOpened.getHasConflicts())) {
					throw new GitlabException(String.format("MR#%s n??o pode ser aceito pois apresenta conflitos de integra????o", mrOpened.getIid()));
				}

				if (!mrOpened.getMergeStatus().equals(GitlabMergeRequestStatusEnum.CAN_BE_MERGED)) {
					throw new GitlabException(String.format("MR#%s n??o pode ser aceito pois apresenta status igual a %s", mrOpened.getIid(), mrOpened.getMergeStatus()));
				}

				GitlabAcceptMRRequest acceptMerge = new GitlabAcceptMRRequest();
				acceptMerge.setMergeRequestIid(mrOpened.getIid());
				acceptMerge.setId(projectId);
				acceptMerge.setMergeWhenPipelineSucceeds(true);
				acceptMerge.setSquash(squashCommits);
				acceptMerge.setShouldRemoveSourceBranch(removeSourceBranch);

				return this.acceptMergeRequest(projectId, mrIID, acceptMerge);
			}
		} else {
			throw new GitlabException(String.format("MR#%s n??o existe para o projeto cujo id ?? %s", mrIID, projectId));
		}
	}
	
	public GitlabMRResponse atualizaLabelsMR(GitlabMergeRequestAttributes mergeRequest, List<String> labels) {
		GitlabMRResponse mergeResponse = null;

		if(labels != null && mergeRequest != null) {
			GitlabMRUpdateRequest updateRequest = new GitlabMRUpdateRequest();
			updateRequest.setMergeRequestIid(mergeRequest.getIid());
			updateRequest.setId(mergeRequest.getId());
			updateRequest.setLabels(String.join(",", labels));
			
			mergeResponse = updateMergeRequest(mergeRequest.getTargetProjectId().toString(), mergeRequest.getIid(), updateRequest);
		} else {
			throw new IllegalArgumentException();
		}
		
		return mergeResponse;
	}

	public GitlabMRResponse removeLabelsMR(GitlabMergeRequestAttributes mergeRequest, List<String> removerLabels) {
		GitlabMRResponse mergeResponse = null;
		if(removerLabels != null && mergeRequest != null) {
			String projectId = mergeRequest.getTargetProjectId().toString();
			BigDecimal mergeRequestIId = mergeRequest.getIid();
			GitlabMRUpdateRequest updateMerge = new GitlabMRUpdateRequest();
			updateMerge.setMergeRequestIid(mergeRequestIId);
			updateMerge.setId(mergeRequest.getId());
			updateMerge.setRemoveLabels(String.join(",", removerLabels));
			
			mergeResponse = updateMergeRequest(projectId, mergeRequestIId, updateMerge);
		}
		
		return mergeResponse;
	}
	
	public GitlabMRResponse updateMergeRequest(String projectId, BigDecimal mergeRequestIId, GitlabMRUpdateRequest updateMerge) {
		GitlabMRResponse mergeResponse = null;

		try {
			mergeResponse = gitlabClient.updateMergeRequest(projectId, mergeRequestIId, updateMerge);
		} catch (Exception e) {
			logger.error("Falha ao atualizar o MR#"+ mergeRequestIId + " do projeto " + projectId, e);
		}

		return mergeResponse;
	}
	
	public List<GitlabMergeRequestPipeline> listMergePipelines(String projectId, BigDecimal mergeRequestIId) {
		List<GitlabMergeRequestPipeline> gitlabMergeRequestPipelines = new ArrayList<>();
		try {
			gitlabMergeRequestPipelines = gitlabClient.listMRPipelines(projectId, mergeRequestIId);
		} catch (Exception e) {
			logger.error("Falha ao recuperar os pipelines do MR#" + mergeRequestIId + " do projeto " + projectId, e);
		}
		
		return gitlabMergeRequestPipelines;
	}

	public GitLabPipeline createMRPipeline(BigDecimal projectId, BigDecimal mergeRequestIId) {
		GitLabPipeline gitlabPipeline = null;
		try {
			gitlabPipeline = gitlabClient.createMRPipeline(projectId, mergeRequestIId);
		} catch (Exception e) {
			logger.error("Falha ao criar novo pipeline do MR#" + mergeRequestIId + " do projeto " + projectId, e);
		}

		return gitlabPipeline;
	}
	
	public GitlabMRResponse openMergeRequest(String projectId, GitlabMRRequest mergeRequest) {
		GitlabMRResponse mergeResponse = null;

		try {
			mergeResponse = gitlabClient.createMergeRequest(projectId, mergeRequest);
		} catch (Exception e) {
			logger.error("Falha ao abrir MR para o branch {}. Erro: {}", mergeRequest.getSourceBranch(), e.getLocalizedMessage());
		}

		return mergeResponse;
	}
	
	public GitlabMRResponse acceptMergeRequest(String projectId, BigDecimal mergeRequestIId, GitlabAcceptMRRequest acceptMerge) throws GitlabException {
		GitlabMRResponse mergeResponse = null;

		try {
			mergeResponse = gitlabClient.acceptMergeRequest(projectId, mergeRequestIId, acceptMerge);
		} catch (Exception e) {
			throw new GitlabException(String.format("Falha ao aceitar o MR#%s do projeto %s. Erro: %s", mergeRequestIId, projectId, e.getLocalizedMessage()));
		}

		return mergeResponse;
	}

	public GitlabDiscussion sendMergeRequestDiscussionThread(String projectId, BigDecimal mergeRequestIId, String body) throws Exception {
		GitlabDiscussion mergeResponse = null;
		try {
			GitlabMRCommentRequest mergeComment = new GitlabMRCommentRequest(new BigDecimal(projectId), mergeRequestIId, body);
			mergeResponse = gitlabClient.createMergeRequestDiscussion(projectId, mergeRequestIId, mergeComment);
		}catch (Exception e) {
			String errorMessage = "Falhou ao tentar criar uma discuss??o no MR: !"+ mergeRequestIId 
				+ " - no projeto: " + projectId + " erro: " + e.getLocalizedMessage();
			logger.error(errorMessage);
//			telegramService.sendBotMessage(errorMessage);
			
			throw new Exception(errorMessage);
		}
		return mergeResponse;
	}

	public GitlabNote sendMergeRequestComment(String projectId, BigDecimal mergeRequestIId, String body) {
		GitlabNote mergeResponse = null;
		try {
			GitlabMRCommentRequest mergeComment = new GitlabMRCommentRequest(new BigDecimal(projectId), mergeRequestIId, body);
			mergeResponse = gitlabClient.createMergeRequestNote(projectId, mergeRequestIId, mergeComment);
		}catch (Exception e) {
			logger.error("Falha ao criar um coment??rio no MR#" + mergeRequestIId + " do projeto: " + projectId, e);
		}
		return mergeResponse;
	}
	
	public List<GitlabNote> listAllMergeRequestNotes(String projectId, BigDecimal mergeRequestIId) {
		List<GitlabNote> gitlabNotes = new ArrayList<>();
		try {
			gitlabNotes = gitlabClient.listAllMergeRequestNotes(projectId, mergeRequestIId);
		}catch (Exception e) {
			logger.error("Falha ao recuperar coment??rios do MR#" + mergeRequestIId + " do projeto: " + projectId, e);
		}
		return gitlabNotes;
	}

	public GitlabCommitResponse atualizaVersaoPom(String projectId, String branchName, String newVersion, String actualVersion, String commitMessage) {
		GitlabCommitResponse response = null;

		if (StringUtils.isNotBlank(newVersion) && StringUtils.isNotBlank(actualVersion) && !actualVersion.equalsIgnoreCase(newVersion)) {
			String pomContent = this.getRawFile(projectId, branchName, POMXML);
			if (StringUtils.isNotBlank(pomContent)) {
				String projectVersionTagname = this.getProjectVersionTagName(projectId);
				String pomContentChanged = GitlabUtils.changePomXMLVersion(actualVersion, newVersion, pomContent, projectVersionTagname);
				if (!pomContent.equals(pomContentChanged)) {
					response = sendTextAsFileToBranch(projectId, branchName, POMXML, pomContentChanged, commitMessage);
				} else {
					logger.info("N??o h?? necessidade de altera????o do conte??do do arquivo {} do projeto {} n??o foi alterado", POMXML, projectId);
				}
			}
		}
		return response;
	}

	public String getVersion(BigDecimal projectId, String branchName, Boolean onlyNumbers) {
		return getVersion(projectId.toString(), branchName, onlyNumbers);
	}
	
	public String getVersion(String projectId, String branchName, boolean onlyNumbers) {
		String version = null;

		if (projectId.equals("180")) { // pje2-web
			String jsonContent = this.getRawFile(projectId, branchName, PACKAGE_JSON);
			if (StringUtils.isNotEmpty(jsonContent)) {
				try {
					version = Utils.getElementFromJSON(jsonContent);
					version = onlyNumbers ? Utils.clearVersionNumber(version): version;
				} catch (JsonProcessingException ex) {
					logger.error("N??o foi poss??vel recuperar o n??mero da vers??o no arquivo {}. {}", PACKAGE_JSON, ex.getLocalizedMessage());
				}
			}
		} else {
			String pomContent = this.getRawFile(projectId, branchName,  POMXML);
			String projectVersionTagname = this.getProjectVersionTagName(projectId);
			
			if (StringUtils.isNotEmpty(pomContent)) {
				version = Utils.getElementFromXML(pomContent, projectVersionTagname);
			}

			if (StringUtils.isEmpty(version)) {
				logger.error("N??o foi poss??vel recuperar o n??mero da vers??o no arquivo {} para o caminho {}", POMXML, projectVersionTagname);
			} else {
				version = onlyNumbers ? Utils.clearVersionNumber(version): version;
			}
		}
		return version;
	}
	
	public void atualizaNumeracaoPastaScriptsVersao(String projectId, String branchName, String newVersion, String actualVersion, String commitMessage) {
		if(StringUtils.isNotBlank(newVersion) && StringUtils.isNotBlank(actualVersion)) {
			actualVersion = Utils.clearVersionNumber(actualVersion);
			if(!actualVersion.equalsIgnoreCase(newVersion)) {
				String previousPath = GitlabService.SCRIPS_MIGRATION_BASE_PATH + actualVersion;
				String newPath = GitlabService.SCRIPS_MIGRATION_BASE_PATH + newVersion;
				
				renameDir(projectId, branchName, previousPath, newPath, commitMessage, actualVersion, newVersion);
			}
		}
	}

	public String getProjectVersionTagName(String projectId) {
		GitlabProjectVariable projectVariable = this.getProjectVariable(projectId, PROJECT_PROPERTY_POM_VERSION_TAGNAME);
		return projectVariable != null ? projectVariable.getValue() : POM_TAGNAME_PROJECT_VERSION_DEFAULT;
	}
	
	public String getJiraProjectKey(String projectId){
		String jiraProjectKey = null;

		GitlabProjectVariable gitlabProjectVariable = this.getProjectVariable(projectId, JIRA_PROJECT_KEY);

		if(gitlabProjectVariable != null) {
			jiraProjectKey = gitlabProjectVariable.getValue();
		}

		return jiraProjectKey;
	}
	
	public GitlabProjectVariable getProjectVariable(String projectId, String variableKey) {
		GitlabProjectVariable gitlabProjectVariable = null;

		try {
			gitlabProjectVariable = this.gitlabClient.getSingleProjectVariable(projectId, variableKey);
		} catch (Exception e) {
			logger.error("N??o foi poss??vel recuperar a variavel {} do projeto {}. Erro: {}", variableKey, projectId, e.getLocalizedMessage());
		}

		return gitlabProjectVariable;
	}
	
	public String changePomVersion(String projectId, String branchRef, String versaoAtual, String nextVersion, VersionTypeEnum versionType, 
			String commitMessage, MessagesLogger messages) throws Exception {
		String lastCommitId = null;
		// verifica se ?? necess??rio alterar o POM
		if(StringUtils.isNotBlank(versaoAtual) && StringUtils.isNotBlank(nextVersion)) {
			// ?? necess??rio alterar o POM
			String nextVersionPom = nextVersion;
			switch(versionType) {
			case SNAPSHOT:
				nextVersionPom += "-SNAPSHOT";
				break;
			case RELEASECANDIDATE:
				nextVersionPom += "-RELEASE-CANDIDATE";
				break;
			default:
				nextVersionPom = nextVersion;
			}
			if(!versaoAtual.equalsIgnoreCase(nextVersionPom)) {
				GitlabCommitResponse response = atualizaVersaoPom(projectId, branchRef, 
						nextVersionPom, versaoAtual, commitMessage);
				if(response != null) {
					logger.info("Atualizada a vers??o do POM.XML de: " + versaoAtual + " - para: " + nextVersionPom);
					lastCommitId = response.getId();
				}else {
					String errorMessage = "Falhou ao tentar atualizar o POM.XML de: " + versaoAtual + " - para: " + nextVersionPom;
					logger.error(errorMessage);
					slackService.sendBotMessage(errorMessage);
					rocketchatService.sendBotMessage(errorMessage);
//					telegramService.sendBotMessage(errorMessage);
					throw new Exception(errorMessage);
				}
			}else {
				logger.info("O POM.XML j?? est?? atualizado");
			}
		}
		return lastCommitId;
	}

	public GitlabUser findUserByEmail(String email) {
		Map<String, String> options = new HashMap<>();
		if(StringUtils.isNotBlank(email)) {
			options.put("search", email);
		}
		List<GitlabUser> users = findUsers(options);
		GitlabUser user = (users != null && !users.isEmpty()) ? users.get(0) : null;
		return user;
	}

	public GitlabUser findUserById(String id) {
		Map<String, String> options = new HashMap<>();
		if(StringUtils.isNotBlank(id) && StringUtils.isNumeric(id)) {
			options.put("id", id);
		}
		List<GitlabUser> users = findUsers(options);
		GitlabUser user = (users != null && !users.isEmpty()) ? users.get(0) : null;
		return user;
	}
	
	public List<GitlabUser> findGitlabUsers(String searchValue) {
		Map<String, String> options = new HashMap<>();
		if(StringUtils.isNotBlank(searchValue)) {
			if(StringUtils.isNumeric(searchValue)) {
				options.put("id", searchValue);
			}else {
				options.put("search", searchValue);
			}
		}
		
		return findUsers(options);
	}

	public GitlabUser findUserByUsername(String username) {
		List<GitlabUser> users = findUsers(Collections.singletonMap("username", username));
		return users != null && !users.isEmpty() ? users.get(0) : null;
	}

	public List<GitlabUser> findUsers(Map<String, String> options) {
		List<GitlabUser> users = null;
		try {
			users = gitlabClient.findUser(options);
		} catch (Exception e) {
			logger.error("Falha ao recuperar dados de usu??rio", e);
		}
		return users;
	}
	
	public GitlabUser getLastCommitAuthor(GitlabMergeRequestAttributes mergeRequest) {
		GitlabUser user = null;
		if(mergeRequest != null) {
			GitlabCommit lastCommit = mergeRequest.getLastCommit();
			if(lastCommit != null) {
				GitlabCommitAuthor commitAuthor = lastCommit.getAuthor();
				if(commitAuthor != null && StringUtils.isNotBlank(commitAuthor.getEmail())) {
					user = findUserByEmail(commitAuthor.getEmail());
				}
			}
		}
		
		return user;
	}
	public GitlabCommitResponse atualizaBranchNovaVersao(String gitlabProjectId, String branchName, String commitMessage, String actualVersion, String newVersion) throws Exception {
		String pomContent = getRawFile(gitlabProjectId, branchName, POMXML);

		if (StringUtils.isNotBlank(pomContent)) {
			String projectVersionPath = getProjectVersionTagName(gitlabProjectId);
			String pomContentChanged = GitlabUtils.changePomXMLVersion(actualVersion, newVersion + "-SNAPSHOT", pomContent, projectVersionPath);
			
			GitlabCommitActionRequest changeVersionNumberAction = new GitlabCommitActionRequest();
			changeVersionNumberAction.setAction(GitlabCommitActionsEnum.UPDATE);
			changeVersionNumberAction.setFilePath(POMXML);
			changeVersionNumberAction.setContent(pomContentChanged);
		
			GitlabCommitActionRequest createScriptsDirectoryAction = new GitlabCommitActionRequest();	
			createScriptsDirectoryAction.setAction(GitlabCommitActionsEnum.CREATE);
			createScriptsDirectoryAction.setFilePath(SCRIPS_MIGRATION_BASE_PATH + newVersion + "/PJE_" + newVersion + FIRST_SCRIPT_SUFFIX);
			createScriptsDirectoryAction.setContent("-- Arquivo inicial dos scripts da versao " + newVersion);
		
			GitlabCommitRequest commit = new GitlabCommitRequest();
			commit.setBranch(branchName);
			commit.commitMessage(commitMessage);
			commit.getActions().add(changeVersionNumberAction);
			commit.getActions().add(createScriptsDirectoryAction);
			
			return gitlabClient.sendCommit(gitlabProjectId, commit);
		}
		throw new Exception("N??o foi poss??vel recuperar o conte??do do arquivo " + POMXML + " para o branch " + branchName);
	}
}