package dev.pje.bots.apoiadorrequisitante.handlers.docs;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.model.bot.ProcessingMessage;
import com.devplatform.model.bot.VersionReleaseNotes;
import com.devplatform.model.gitlab.GitlabProjectExtended;
import com.devplatform.model.gitlab.GitlabTagRelease;
import com.devplatform.model.gitlab.response.GitlabBranchResponse;
import com.devplatform.model.gitlab.response.GitlabCommitResponse;
import com.devplatform.model.gitlab.response.GitlabMRResponse;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.JiraIssueAttachment;
import com.devplatform.model.jira.event.JiraEventIssue;
import com.devplatform.model.jira.vo.JiraEstruturaDocumentacaoVO;

import dev.pje.bots.apoiadorrequisitante.exception.GitlabException;
import dev.pje.bots.apoiadorrequisitante.handlers.Handler;
import dev.pje.bots.apoiadorrequisitante.handlers.MessagesLogger;
import dev.pje.bots.apoiadorrequisitante.services.GitlabService;
import dev.pje.bots.apoiadorrequisitante.services.JiraService;
import dev.pje.bots.apoiadorrequisitante.utils.Utils;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.AsciiDocMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.DocusaurusMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.markdown.GitlabMarkdown;
import dev.pje.bots.apoiadorrequisitante.utils.textModels.ReleaseNotesTextModel;

@Component
public class Documentation02CreateSolutionHandler extends Handler<JiraEventIssue>{
	
	private static final Map<String, String> DOC_PATH;

	private static final Logger logger = LoggerFactory.getLogger(Documentation02CreateSolutionHandler.class);

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public String getMessagePrefix() {
		return "|DOCUMENTATION||02||CREATE-SOLUTION|";
	}

	@Override
	public int getLogLevel() {
		return MessagesLogger.LOGLEVEL_INFO;
	}

	@Value("${project.documentation.url}")
	private String DOCSURL;
	
	@Autowired
	private ReleaseNotesTextModel releaseNotesModel;
	
	static {
		DOC_PATH = new HashMap<String, String>();
		DOC_PATH.put("7", "src/pages/servicos-negociais/servico-pje-legacy/notas-da-versao/index.mdx");
		DOC_PATH.put("783", "src/pages/servicos-auxiliares/servico-mni-client/notas-da-versao/index.mdx");
	}

	/**
	 * Criar documentacao relacionada ?? issue
	 * 
	 * ok 1. obtem os dados da estrutura de documentacao
	 * ok 2. traduz os dados da estrutura de documentacao em paths: path principal + path de anexos (assets)
	 * - verifia se a documenta????o ser?? manual ou automatizada:
	 * -- manual:: tipo: n??o ?? release notes e se tiver um branh e n??o tiver um documento .adoc como anexo
	 * 
	 * 
	 * ok 3. identifica o nome do arquivo principal de documentacao com seu path
	 * ok 4. cria o branch da issue atual - feature branch atual
	 * ok 5. verifica se j?? existe um documento principal com o conte??do atual
	 * ok 6. cria a pasta de anexos (assets) e adiciona todos os demais documentos da issue l??
	 * ok 7. se for release notes:
	 * ok 7.1 adiciona o release na lista de releases concatenadas
	 * ok 7.2 adiciona o release na lista de releases com links separados
	 * TODO - ADICIONA NO SITE DA ELISA AQUI
	 * ok 8. cria um path do link de documentacao (apenas sem o host:porta)
	 * 10. Proxima atividade
	 *  
	 * @param issue
	 * @param estruturaDocumentacao
	 * @return
	 * @throws ParseException
	 */

	@Override
	public void handle(JiraEventIssue jiraEventIssue) throws Exception {
	}
	public List<ProcessingMessage> atualizaDocs(String gitlabProjectId, VersionReleaseNotes releaseNotes) {
		messages.clean();
		
		this.atualizaReleaseNotesGitLab(releaseNotes);
		
		GitlabProjectExtended gitlabProject = this.gitlabService.getProjectDetails(gitlabProjectId);
		this.atualizaDocsPje(gitlabProject.getName(), releaseNotes);
		
//		if (jiraEventIssue != null && jiraEventIssue.getIssue() != null) {
//			JiraIssue issue = jiraEventIssue.getIssue();
//			// 1. verifica se ?? do tipo "geracao de nova versao"
//			if(	(
//					JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_DOCUMENTATION)
//					|| JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES)
//					) &&
//					JiraUtils.isIssueInStatus(issue, JiraService.STATUS_DOCUMENTATION_PROCESSING_ID) &&
//					JiraUtils.isIssueChangingToStatus(jiraEventIssue, JiraService.STATUS_DOCUMENTATION_PROCESSING_ID)) {
//
//				messages.setId(issue.getKey());
//				messages.debug(jiraEventIssue.getIssueEventTypeName().name());
//				issue = jiraService.recuperaIssueDetalhada(issue.getKey());
//
//				if(issue.getFields() != null && issue.getFields().getProject() != null) {
//					// verifica se informou o caminho da documentacao corretamente
//					JiraEstruturaDocumentacaoVO estruturaDocumentacao = new JiraEstruturaDocumentacaoVO(
//							issue.getFields().getEstruturaDocumentacao(), 
//							issue.getFields().getNomeParaExibicaoDocumentacao(), issue.getFields().getPathDiretorioPrincipal());
//
//					if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES) && estruturaDocumentacao.getEstruturaInformadaManualmente()) {
//						messages.error("Para demandas de documenta????o de release notes, deve-se selecionar as op????es de estrutura de documenta????o atualmente existentes (e n??o 'Outros')");
//					}else {
//						if(!estruturaDocumentacao.getEstruturaInformadaValida()) {
//							if(estruturaDocumentacao.getCategoriaIndicadaOutros() || estruturaDocumentacao.getSubCategoriaIndicadaOutros()) {
//								messages.error("H?? um problema na identifica????o da estrutura de documenta????o, por favor, especifique a informa????o nos campos: 'nome para exibi????o da documenta????o' e 'path do diret??rio principal', essa informa????o ser?? usada para a publica????o da documenta????o.");
//							}else {
//								messages.error("H?? um problema na identifica????o da estrutura de documenta????o, por favor, indique a informa????o completa corretamente, ela ser?? usada para a publica????o da documenta????o.");
//							}
//						}
//					}
//
//					/**
//					 * verifica se h?? branch relacionado, se houver, n??o valida os anexos e utilizar?? o branch como refer??ncia
//					 */
//					String branchName = recuperarBranchRelacionado(issue);
//
//					/**
//					 * valida anexos:
//					 * - se foram indicados anexos, pelo menos 1 anexo com a extensao .adoc deve existir - caso contr??rio gera erro para o usuario
//					 */
//					JiraIssueAttachment anexoAdoc = recuperaAnexoDocumentoPrincipalAdoc(issue);
//					if (anexoAdoc == null){
//						if(StringUtils.isBlank(branchName)) {
//							messages.error("Deve haver um e apenas um arquivo anexo com a extens??o: " + JiraService.FILENAME_SUFFIX_ADOC 
//									+ " - ele ser?? utilizado como documento principal da documenta????o criada.\n"
//									+ "Alternativamente pode-se indicar um branch com c??digo da implementa????o manual.");
//						}
//					}
//
//					String versaoASerLancada = issue.getFields().getVersaoSeraLancada();
//					if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES) && StringUtils.isBlank(versaoASerLancada)){
//						messages.error("A indica????o da vers??o a ser lan??ada ?? obrigat??ria, indique apenas uma vers??o.");
//					}
//
//					String jiraProjectKey = issue.getFields().getProject().getKey();
//					if(StringUtils.isBlank(jiraProjectKey)) {
//						messages.error("N??o foi poss??vel identificar a chave do projeto atual");
//					}
//					String gitlabProjectId = jiraService.getGitlabProjectFromJiraProjectKey(jiraProjectKey);
//					if(StringUtils.isBlank(gitlabProjectId)) {
//						messages.error("N??o foi poss??vel identificar qual ?? o reposit??rio no gitlab para este projeto do jira.");
//					}
//
//
//					GitlabBranchResponse branchResponse = null;
//					if(StringUtils.isNotBlank(branchName)) {
//						branchResponse = gitlabService.getSingleRepositoryBranch(gitlabProjectId, branchName);
//					}
//
//					String documentationURL = null;
//					String MRsAbertos = issue.getFields().getMrAbertos();
//					String MrsAbertosConfirmados = gitlabService.checkMRsOpened(MRsAbertos);
//
//					Boolean documentacaoManual = (
//							!JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES) 
//							&& branchResponse != null && anexoAdoc == null);
//
//					if(!messages.hasSomeError()) {
//						// identifica o path e o nome de exibicao da categoria e da subcategoria
//						if(estruturaDocumentacao.getSubCategoriaValida() && !estruturaDocumentacao.getCategoriaIndicadaOutros()) {
//							String subCategoriaPath = translateSubCategoryToPathName(estruturaDocumentacao.getSubCategoriaValue(), gitlabProjectId);
//							estruturaDocumentacao.setSubCategoriaPathDiretorio(subCategoriaPath);
//							estruturaDocumentacao.setSubCategoriaNomeParaExibicao(estruturaDocumentacao.getSubCategoriaValue());
//						}
//						if(!estruturaDocumentacao.getCategoriaIndicadaOutros()) {
//							String categoriaPath = translateCategoryToPathName(estruturaDocumentacao.getCategoriaValue());
//							estruturaDocumentacao.setCategoriaPathDiretorio(categoriaPath);
//							estruturaDocumentacao.setCategoriaNomeParaExibicao(estruturaDocumentacao.getCategoriaValue());
//						}
//
//						if(!documentacaoManual) {
//							// Para o tipo de documentacao release notes, o path refer??ncia ser?? um subpath do projeto principal
//							String pathSrcPreffix = PATHNAME_SRC_DOCUMENTACAO;
//							String projectPath = null;
//							String documentoPrincipalNomeAdoc = null;
//							if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES)) {
//								String fullPath = estruturaDocumentacao.getSubCategoriaPathDiretorio();
//								if(fullPath != null) {
//									projectPath = fullPath;
//									if(fullPath.contains("html")) {
//										projectPath = Utils.getPathFromFilePath(fullPath);
//									}
//									projectPath += "/" + RELEASE_NOTES_DIR + "/" + RELEASE_NOTES_INCLUDES_DIR;
//									documentoPrincipalNomeAdoc = anexoAdoc.getFilename();
//								}
//							}else {
//								// Para outros tipos de documentacao o path refer??ncia ser?? exatamente o projeto principal
//								if(estruturaDocumentacao.getSubCategoriaValida()) {
//									projectPath = estruturaDocumentacao.getSubCategoriaPathDiretorio();
//								}else {
//									projectPath = estruturaDocumentacao.getCategoriaPathDiretorio();
//								}
//								if(StringUtils.isNotBlank(projectPath) && projectPath.contains("html")) {
//									String fileNameHtml = Utils.getFileNameFromFilePath(projectPath);
//									documentoPrincipalNomeAdoc = fileNameHtml.replace(JiraService.FILENAME_SUFFIX_HTML, JiraService.FILENAME_SUFFIX_ADOC);
//
//									projectPath = Utils.getPathFromFilePath(projectPath);
//								}else {
//									if(anexoAdoc != null) {
//										documentoPrincipalNomeAdoc = anexoAdoc.getFilename();
//									}
//								}
//							}
//
//							messages.info("Identificado o path: " + projectPath + " e o arquivo principal: " + documentoPrincipalNomeAdoc);
//
//							// 4. cria o branch da issue atual - feature branch atual
//							if(branchResponse == null) {
//								if(StringUtils.isBlank(branchName)) {
//									branchName = issue.getKey();
//								}
//								branchResponse = gitlabService.createFeatureBranch(gitlabProjectId, branchName);
//								if(branchResponse != null && branchName.equals(branchResponse.getBranchName())) {
//									messages.info("Feature branch: " + branchName + " criado no projeto: " + gitlabProjectId);
//								}else {
//									messages.error("Erro ao tentar criar o feature branch: " + branchName + " no projeto: " + gitlabProjectId);
//								}
//							}else {
//								messages.info("Feature branch: " + branchName + " j?? existente no projeto: " + gitlabProjectId);
//							}

//							if(!messages.hasSomeError()) {
								// 5. verifica se j?? existe um documento principal com o conte??do atual
								// recupera o documento atual
//								byte[] textFile = jiraService.getAttachmentContent(issue, documentoPrincipalNomeAdoc);
//								String texto = null;
//								if(textFile != null) {
//									texto = new String(textFile);
//								}
								
//								releaseNotesModel.setReleaseNotes(releaseNotes);
//								String texto = releaseNotesModel.convert(new AsciiDocMarkdown());
								
//								String adocFilePath = Utils.normalizePaths(pathSrcPreffix  + "/" + projectPath + "/" + documentoPrincipalNomeAdoc);

								// verifica se j?? existe o arquivo no destino e se o texto ?? diferente, caso contr??rio, mantem como est??
//								String conteudoArquivo = gitlabService.getRawFile(gitlabProjectId, adocFilePath, branchName);
//
//								if(StringUtils.isBlank(conteudoArquivo) || (!Utils.compareAsciiIgnoreCase(conteudoArquivo, texto))) {
////									String textoCommit = getCommitText(estruturaDocumentacao, JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES), versaoASerLancada);
//									if(!messages.hasSomeError()) {
////										String commitMessage = "[" + issue.getKey() + "] " + textoCommit;
//										String commitMessage = "[RELEASE] Gera????o do release notes para o PJe vers??o 2.2.0.1";
//										// criar o arquivo principal da documentacao no branch
//										GitlabCommitResponse commitResponse = gitlabService.sendTextAsFileToBranch(gitlabProjectId, branchResponse, adocFilePath,
//												texto, commitMessage);
//										if(commitResponse != null) {
//											messages.info("Criado o arquivo: " + adocFilePath + " no branch: " + branchName + " do projeto: " + gitlabProjectId);
//											messages.debug(commitResponse.toString());
//										}else {
//											messages.error("Erro ao tentar criar o arquivo: " + adocFilePath + " no branch: " + branchName + " do projeto: " + gitlabProjectId);
//										}
//									}
//								}else {
//									messages.info("Arquivo: " + adocFilePath + " j?? existe no branch: " + branchName + " do projeto: " + gitlabProjectId);
//								}

//								if(!messages.hasSomeError()) {
//									// 6. cria a pasta de anexos (assets) e adiciona todos os demais documentos da issue l??
//									List<JiraIssueAttachment> demaisAnexos = recuperaDemaisAnexosNaoAdoc(issue);
//									if(demaisAnexos != null && !demaisAnexos.isEmpty()) {
//										String assetsPath = pathSrcPreffix  + "/" + projectPath + "/" + JiraService.DOCUMENTATION_ASSETS_DIR;
//										List<GitlabCommitFileVO> files = new ArrayList<>();
//										for (JiraIssueAttachment anexo : demaisAnexos) {
//											String fileName = Utils.normalizePaths(assetsPath + "/" + anexo.getFilename());
//											byte[] content = jiraService.getAttachmentContent(issue, anexo.getFilename());
//											String contentBase64 = Utils.byteArrToBase64(content);
//											Boolean isBase64 = true;
//											GitlabCommitFileVO file = new GitlabCommitFileVO(fileName, contentBase64, isBase64);
//											files.add(file);
//										}
//										String commitMessage = "[" + issue.getKey() + "] Adicionando anexos relacionados";
//										GitlabCommitResponse commitResponse = gitlabService.sendFilesToBranch(gitlabProjectId, branchName, files, commitMessage);
//										if(commitResponse != null) {
//											messages.info("Enviados anexos ao branch: " + branchName + " do projeto: " + gitlabProjectId);
//											messages.debug(commitResponse.toString());
//										}else {
//											messages.error("Erro ao tentar enviar anexos ao branch: " + branchName + " do projeto: " + gitlabProjectId);
//										}
//									}
//								}
//
//								if(!messages.hasSomeError()) {
//									if(JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES)) {
//										// * 7.1 adiciona o release na lista de releases concatenadas
//										// * 7.2 adiciona o release na lista de releases com links separados									
//										String documentoPrincipalNomeHtml = documentoPrincipalNomeAdoc.replace(JiraService.FILENAME_SUFFIX_ADOC, JiraService.FILENAME_SUFFIX_HTML);
//										String projectPathSubdir = projectPath;
//										if(projectPathSubdir.endsWith(RELEASE_NOTES_INCLUDES_DIR)) {
//											projectPathSubdir = projectPathSubdir.replaceAll(RELEASE_NOTES_INCLUDES_DIR, "/");
//										}
//										if(!messages.hasSomeError()) {
//											String pathReleaseNotesCompleto = Utils.normalizePaths(pathSrcPreffix  + "/" + projectPathSubdir + "/" + RELEASE_NOTES_RELEASE_COMPLETO);
//											adicionaNovoArquivoReleaseNotesNoReleaseCompleto(issue, gitlabProjectId, branchName, documentoPrincipalNomeAdoc, pathReleaseNotesCompleto);
//										}
//										if(!messages.hasSomeError()) {
//											String pathReleaseNotesLista = Utils.normalizePaths(pathSrcPreffix  + "/" + projectPathSubdir + "/" + RELEASE_NOTES_RELEASE_LISTA);
//											String pathReleaseNotesLista = "src/main/asciidoc/servicos-negociais/pje-legacy/release-notes/index.adoc";

//											adicionaNovoArquivoReleaseNotesNaListaDeReleases(issue, gitlabProjectId, branchName, documentoPrincipalNomeHtml,
//													versaoASerLancada, pathReleaseNotesLista);
											
//											adicionaNovoArquivoReleaseNotesNaListaDeReleases(gitlabProjectId, branchName, documentoPrincipalNomeHtml, versaoASerLancada, pathReleaseNotesLista);
//										}
//										if(!messages.hasSomeError()) {
//											String serverAddressTemplate = DOCSURL;
//											documentationURL = createDocumentationLink(serverAddressTemplate, documentoPrincipalNomeHtml, Utils.getPathFromFilePath(adocFilePath));
//										}
//									}
//								}
//							}
//						}
//					}

//					if(!messages.hasSomeError()) {
////						String textoCommit = getCommitText(estruturaDocumentacao, JiraUtils.isIssueFromType(issue, JiraService.ISSUE_TYPE_RELEASE_NOTES), versaoASerLancada);
////
////						String mergeMessage = "[" + issue.getKey() + "] " + textoCommit;
//						GitlabMRResponse mrResponse = null;
//						String erro = null;
//						try {
////							mrResponse = gitlabService.openMergeRequestIntoBranchDefault(gitlabProjectId, branchName, mergeMessage);
//							mrResponse = gitlabService.openMergeRequestIntoBranchDefault(gitlabProjectId, branchName, "[RELEASE] Gera????o do release notes para o PJe vers??o " + releaseNotes.getVersion());
//							
//
//							GitlabMRResponse response = gitlabService.acceptMergeRequest(gitlabProjectId, mrResponse.getIid());
//							if(response == null) {
//								messages.error("Falhou ao tentar aprovar o MR: "+ mrResponse.getIid() + " - do projeto: " + gitlabProjectId);
//							}
//						} catch (Exception e) {
//							erro = e.getLocalizedMessage();
//							messages.error("Houve um problema ao abrir o merge do branch: " + branchName + " do projeto: " + gitlabProjectId);
//							if(StringUtils.isNotBlank(erro)) {
//								messages.error(erro);
//							}
//						}
////						if(mrResponse != null) {
////							messages.info("MR " + mrResponse.getIid() + " aberto para o branch: " + branchName + " no projeto: " + gitlabProjectId);
////							MrsAbertosConfirmados = Utils.concatenaItensUnicosStrings(MrsAbertosConfirmados, mrResponse.getWebUrl());
////							messages.debug(mrResponse.toString());
////						}
//					}

//					JiraMarkdown jiraMarkdown = new JiraMarkdown();
//					StringBuilder textoComentario = new StringBuilder(messages.getMessagesToJira());
//					if(!documentacaoManual) {
//						textoComentario.append(jiraMarkdown.newLine());
//						textoComentario.append(jiraMarkdown.block("Caso se pretenda utilizar anexos (imagem ou outro formato) deve-se utilizar no arquivo principal de documenta????o .adoc, refer??ncias"
//								+ " ?? pasta '" + JiraService.DOCUMENTATION_ASSETS_DIR + "', pois todos os documentos anexados a esta issue que n??o sejam .adoc ser??o enviados ao reposiorio na pasta"
//								+ " '" + JiraService.DOCUMENTATION_ASSETS_DIR + "' no mesmo path do arquivo principal."));
//					}
//					if(messages.hasSomeError()) {
//						// indica que h?? pend??ncias - encaminha ao demandante
//						Map<String, Object> updateFields = new HashMap<>();
//						jiraService.adicionarComentario(issue, textoComentario.toString(), updateFields);
//						enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_IMPEDIMENTO, true, true);
//					}else {
//						// tramita automaticamente, enviando as mensagens nos coment??rios
//						Map<String, Object> updateFields = new HashMap<>();
//						// adiciona a URL relacionada
//						jiraService.atualizarURLPublicacao(issue, documentationURL, updateFields);
//						// adiciona o nome do branch relacionado
//						jiraService.atualizarBranchRelacionado(issue, branchName, updateFields, false);
//						// adiciona o MR aberto
//						jiraService.atualizarMRsAbertos(issue, MrsAbertosConfirmados, updateFields, true);
//
//						jiraService.adicionarComentario(issue, textoComentario.toString(), updateFields);
//
//						enviarAlteracaoJira(issue, updateFields, null, JiraService.TRANSITION_PROPERTY_KEY_SOLICITAR_HOMOLOGACAO, true, true);
//					}
//				}
//
//			}
//		}
		return messages.messages;
	}
	
	private void atualizaReleaseNotesGitLab(VersionReleaseNotes releaseNotes) {
		if (releaseNotes != null) {
			releaseNotesModel.setReleaseNotes(releaseNotes);
			String releaseText = releaseNotesModel.convert(new GitlabMarkdown());

			GitlabTagRelease tagReleaseResponse = gitlabService.createTagRelease(releaseNotes.getGitlabProjectId(), releaseNotes.getVersion(), releaseText);
			if (tagReleaseResponse != null) {
				messages.info("Criado documento de release da tag do projeto: " + releaseNotes.getProject());
			} else {
				messages.error("Erro ao criar documento de release da tag do projeto: " + releaseNotes.getProject());
			}
		}
	}
	
	private void atualizaDocsPje(String projectName, VersionReleaseNotes releaseNotes) {
		String gitlabProjectId = "276"; // https://git.cnj.jus.br/pje2/pje2-documentacao
		
		releaseNotesModel.setReleaseNotes(releaseNotes);
		String texto = releaseNotesModel.convert(new DocusaurusMarkdown());
		
		String pathToReleaseNotesPage = DOC_PATH.get(releaseNotes.getGitlabProjectId());
		
		String conteudoArquivo = gitlabService.getRawFile(gitlabProjectId, GitlabService.BRANCH_MASTER, pathToReleaseNotesPage);

		if (conteudoArquivo == null) {
			messages.error(String.format("Arquivo %s n??o encontrado no branch %s do projeto %s", pathToReleaseNotesPage, GitlabService.BRANCH_MASTER, gitlabProjectId));
		} else {
			if (conteudoArquivo.contains(texto)) {
				messages.info(String.format("O conte??do que se pretende incluir j?? consta no branch %s", GitlabService.BRANCH_MASTER));
			} else {
				String branchName = "release-notes-" + projectName + "-" +releaseNotes.getVersion();

				GitlabBranchResponse branchResponse = this.gitlabService.createFeatureBranch(gitlabProjectId, branchName);
				if(branchResponse != null && branchName.equals(branchResponse.getBranchName())) {
					messages.info("Branch: " + branchName + " criado no projeto " + gitlabProjectId);
				}else {
					messages.error("Falha ao criar branch " + branchName + " no projeto " + gitlabProjectId);
				}

				String novoConteudo = conteudoArquivo.replace("<!-- next version -->", texto);
				String commitMessage = String.format("[RELEASE] Gera????o do release notes para o projeto %s vers??o %s", projectName, releaseNotes.getVersion());
				
				GitlabCommitResponse commitResponse = gitlabService.sendTextAsFileToBranch(gitlabProjectId, branchName, pathToReleaseNotesPage, novoConteudo, commitMessage);
				if (commitResponse != null) {
					messages.info("Conte??do do arquivo " + pathToReleaseNotesPage + " atualizado no branch " + branchName + " do projeto " + gitlabProjectId);
				} else {
					messages.error("Falha ao atualizar o arquivo " + pathToReleaseNotesPage + " no branch " + branchName + " do projeto " + gitlabProjectId);
				}

				GitlabMRResponse mrResponse = gitlabService.openMergeRequestIntoBranchDefault(gitlabProjectId, branchName, commitMessage);
				try {
					gitlabService.acceptMergeRequest(gitlabProjectId, mrResponse.getIid(), true, true);
				} catch (GitlabException ex) {
					messages.error(ex.getLocalizedMessage());
				}
			}
		}
	}

	private String recuperarBranchRelacionado(JiraIssue issue) {
		String branchName = null;
		String branchesRelacionados = issue.getFields().getBranchesRelacionados();
		if(StringUtils.isNotBlank(branchesRelacionados)) {
			String[] branches = branchesRelacionados.split(",");
			if(branches.length > 0 && StringUtils.isNotBlank(branches[0])) {
				branchName = branches[0].trim();
			}
		}
		return branchName;
	}

	private JiraIssueAttachment recuperaAnexoDocumentoPrincipalAdoc(JiraIssue issue) {
		JiraIssueAttachment documentoAdoc = null;
		if(issue.getFields().getAttachment() != null && !issue.getFields().getAttachment().isEmpty()) {
			for (JiraIssueAttachment anexo : issue.getFields().getAttachment()) {
				if(anexo.getFilename().endsWith(JiraService.FILENAME_SUFFIX_ADOC)) {
					documentoAdoc = anexo;
					break;
				}
			}
		}

		return documentoAdoc;
	}

	private List<JiraIssueAttachment> recuperaDemaisAnexosNaoAdoc(JiraIssue issue) {
		List<JiraIssueAttachment> outrosAnexos = new ArrayList<>();
		if(issue.getFields().getAttachment() != null && !issue.getFields().getAttachment().isEmpty()) {
			for (JiraIssueAttachment anexo : issue.getFields().getAttachment()) {
				if(!anexo.getFilename().endsWith(JiraService.FILENAME_SUFFIX_ADOC)) {
					outrosAnexos.add(anexo);
				}
			}
		}

		return outrosAnexos;
	}

	private String getCommitText(JiraEstruturaDocumentacaoVO estruturaDocumentacao, boolean isReleaseNotes, String versao) {
		String nomeExibicaoDocumentacao = estruturaDocumentacao.getCategoriaNomeParaExibicao();
		if(StringUtils.isNotBlank(estruturaDocumentacao.getSubCategoriaNomeParaExibicao())) {
			nomeExibicaoDocumentacao = nomeExibicaoDocumentacao + " - " + estruturaDocumentacao.getSubCategoriaNomeParaExibicao();
		}
		String textoCommit = "Gerando documentacao para " + nomeExibicaoDocumentacao;
		if(isReleaseNotes) {
			textoCommit = "Gerando release notes para " + nomeExibicaoDocumentacao;
			textoCommit = textoCommit + " da vers??o: "+ versao;
		}

		return textoCommit;
	}



	private static final String PATHNAME_SRC_DOCUMENTACAO = "src/main/asciidoc/";
	private static final String RELEASE_NOTES_DIR = "/release-notes/";
	private static final String RELEASE_NOTES_INCLUDES_DIR = "/includes/";
	private static final String RELEASE_NOTES_RELEASE_COMPLETO = "release-notes-completo.adoc";
	private static final String RELEASE_NOTES_RELEASE_LISTA = "index.adoc";

	private String createDocumentationLink(String serverAddress, String htmlFileName, String path) {
		String pathHtml = path.replace(PATHNAME_SRC_DOCUMENTACAO, "");
		return serverAddress + Utils.normalizePaths("/" + pathHtml + "/" + htmlFileName);
	}

	/**
	 * Arquivo target: release-notes-completo.adoc <br/>
	 * Item a incluir: <br/>
	 * include::{docdir}/{docsServicePATH}/release-notes/includes/PARAM_RELEASE_NOTES_FILE.adoc[] <br/>
	 * Exemplo de inclusao: <br/>
	 * include::{docdir}/{docsServicePATH}/release-notes/includes/release-notes_2-1-8-0.adoc[] <br/>
	 * <br/>
	 * 
	 * @param issue
	 * @param gitlabProjectId
	 * @param branchName
	 * @param adocFileName
	 * @param pathReleaseNotesCompleto
	 */
	private void adicionaNovoArquivoReleaseNotesNoReleaseCompleto(JiraIssue issue, String gitlabProjectId,
			String branchName, String adocFileName, String pathReleaseNotesCompleto) {

		// 2. obter o arquivo de releases completo
		String releaseNotesCompletoContent = gitlabService.getRawFile(gitlabProjectId, branchName, pathReleaseNotesCompleto);

		if (StringUtils.isNotBlank(releaseNotesCompletoContent)) {
			// 3. verificar se a vers??o j?? est?? l??
			if (!releaseNotesCompletoContent.contains(adocFileName)) {
				// 4. adicionar o include do release
				String linhaIncludeReleaseNotes = "include::{docdir}/{docsServicePATH}/release-notes/includes/"
						+ adocFileName + "[]";
				releaseNotesCompletoContent += "\n" + linhaIncludeReleaseNotes + "\n";

				// 5. identificar a ordem de armazenamento desses includes
				releaseNotesCompletoContent = reordenarListaIncludesReleaseNotesCompleto(releaseNotesCompletoContent);

				String commitMessage = "[" + issue.getKey() + "] Incluindo novo release notes no arquivo de releases completas do projeto";
				GitlabCommitResponse commitResponse = gitlabService.sendTextAsFileToBranch(
						gitlabProjectId, branchName, pathReleaseNotesCompleto,
						releaseNotesCompletoContent, commitMessage);
				if(commitResponse != null) {
					messages.info("Atualizado o arquivo: " + pathReleaseNotesCompleto + " no branch: " + branchName + " do projeto: " + gitlabProjectId);
					messages.debug(commitResponse.toString());
				}else {
					messages.error("Erro ao tentar atualizar o arquivo: " + pathReleaseNotesCompleto + " no branch: " + branchName + " do projeto: " + gitlabProjectId);
				}
			} else {
				messages.info("O include do release notes |" + adocFileName + "| j?? existe no arquivo de releases completas");
			}
		} else {
			messages.error("N??o foi poss??vel encontrar o arquivo " + pathReleaseNotesCompleto + " no projeto " + gitlabProjectId);
		}
	}

	/**
	 * Arquivo target: index.adoc <br/>
	 * Item a incluir: <br/>
	 * 		=== link:includes/PARAM_RELEASE_NOTES_FILE.html[vPARAM_NUMERO_VERSAO] <br/>
	 * Exemplo de inclusao: <br/>
	 * 		=== link:includes/release-notes_2-1-2-3.html[v2.1.2.3] <br/>
	 * <br/>
	 * @param project
	 * @param branch
	 * @param filePath
	 * @param version
	 * @param releaseDate
	 * @throws ParseException
	 */
//	private void adicionaNovoArquivoReleaseNotesNaListaDeReleases(JiraIssue issue, String gitlabProjectId, 
//			String branchName, String htmlFileName, String version, 
//			String pathReleaseNotesLista) throws ParseException {
	private void adicionaNovoArquivoReleaseNotesNaListaDeReleases(String gitlabProjectId, 
			String branchName, String htmlFileName, String version, 
			String pathReleaseNotesLista) {

		// 2. obter o arquivo completo
		String listaReleaseNotesContent = gitlabService.getRawFile(gitlabProjectId, branchName, pathReleaseNotesLista);

		if (StringUtils.isNotBlank(listaReleaseNotesContent)) {
			// 3. verificar se a vers??o j?? est?? l??
			if (!listaReleaseNotesContent.contains(htmlFileName)) {
				// 5. adicionar o include do release
				if (StringUtils.isNotBlank(version)) {
					String linhaIncludeReleaseNotes = "=== link:includes/" + htmlFileName + "[v" + version + "]";
					listaReleaseNotesContent += "\n" + linhaIncludeReleaseNotes + "\n";

					listaReleaseNotesContent = reordenarListaIncludesReleaseNotesHtmls(listaReleaseNotesContent);

//					String commitMessage = "[" + issue.getKey() + "] Incluindo novo release notes no arquivo de lista de releases do projeto";
					String commitMessage = "[RELEASE] Incluindo novo release notes no arquivo de lista de releases do projeto";

					GitlabCommitResponse commitResponse = gitlabService.sendTextAsFileToBranch(
							gitlabProjectId, branchName, pathReleaseNotesLista, listaReleaseNotesContent,
							commitMessage);

					if(commitResponse != null) {
						messages.info("Atualizado o arquivo: " + pathReleaseNotesLista + 
								" no branch: " + branchName + " do projeto: " + gitlabProjectId);
						messages.debug(commitResponse.toString());
					}else {
						messages.error("Erro ao tentar atualizar o arquivo: " + pathReleaseNotesLista + 
								" no branch: " + branchName + " do projeto: " + gitlabProjectId);
					}
				} else {
					messages.error("N??o foi poss??vel identificar a data da release");
				}
			} else {
				messages.info("O include do release notes |" + htmlFileName  + "| j?? existe no arquivo de lista de releases");
			}
		} else {
			messages.error(" N??o foi poss??vel encontrar o arquivo " + pathReleaseNotesLista
					+ " no projeto " + gitlabProjectId);
		}
	}

	private String reordenarListaIncludesReleaseNotesCompleto(String releaseNotesCompletoContent) {
		String[] linhas = releaseNotesCompletoContent.split("\n");
		List<String> linhasArquivoAlterado = new ArrayList<>();
		List<String> releaseNotesIncludes = new ArrayList<>();
		// 1. identifica todos os includes de release notes e mantem no arquivo as
		// informacoes que sejam v??lidas e n??o sejam desses includes
		boolean encontrouAlgumIncludeValido = false;
		for (String linha : linhas) {
			if (linha.trim().startsWith("//")) { // comentarios permanessem onde est??o
				linhasArquivoAlterado.add(linha);
			} else if (linha.toLowerCase().contains(RELEASE_NOTES_INCLUDES_DIR.toLowerCase())
					&& linha.startsWith("include")) {
				encontrouAlgumIncludeValido = true;
				releaseNotesIncludes.add(linha);
				// linhas que n??o sejam de include de release continuam onde est??o, espa??os em
				// branco permanessem onde est??o se n??o estiverem entre os includes de releases
			} else if (StringUtils.isNotBlank(linha) || !encontrouAlgumIncludeValido) {
				linhasArquivoAlterado.add(linha);
			}
		}
		messages.info("Existem " + releaseNotesIncludes.size() + " releases.");
		// 3. ordena os release notes pelo n??mero da vers??o ASC
		Collections.sort(releaseNotesIncludes, new SortReleaseNotesAdocByVersion());
		// 4. remonta o arquivo:
		for (String releaseInclude : releaseNotesIncludes) {
			// 4.2. Grava cada include e adiciona um \n no in??cio e outro no final
			linhasArquivoAlterado.add("\n" + releaseInclude);
		}
		return String.join("\n", linhasArquivoAlterado);
	}

	class SortReleaseNotesAdocByVersion implements Comparator<String> {
		public int compare(String a, String b) {
			String versionAStr = getVersionFromReleaseNotesInclude(a, JiraService.RELEASE_NOTES_FILENAME_PREFIX, JiraService.FILENAME_SUFFIX_ADOC);
			List<Integer> versionA = Utils.getVersionFromString(versionAStr, "-|\\.");
			String versionBStr = getVersionFromReleaseNotesInclude(b, JiraService.RELEASE_NOTES_FILENAME_PREFIX, JiraService.FILENAME_SUFFIX_ADOC);
			List<Integer> versionB = Utils.getVersionFromString(versionBStr, "-|\\.");

			return Utils.compareVersionsDesc(versionA, versionB);
		}
	}

	private String reordenarListaIncludesReleaseNotesHtmls(String releaseNotesCompletoContent) {
		String[] linhas = releaseNotesCompletoContent.split("\n");
		List<String> linhasArquivoAlterado = new ArrayList<>();
		List<String> releaseNotesIncludes = new ArrayList<>();
		// 1. identifica todos os includes de release notes e mantem no arquivo as
		// informacoes que sejam v??lidas e n??o sejam desses includes
		boolean encontrouAlgumIncludeValido = false;
		for (String linha : linhas) {
			if (linha.trim().startsWith("//")) { // comentarios permanessem onde est??o
				linhasArquivoAlterado.add(linha);
			} else if (linha.toLowerCase().contains(("link:includes/" + JiraService.RELEASE_NOTES_FILENAME_PREFIX).toLowerCase())
					&& linha.startsWith("=")) {
				encontrouAlgumIncludeValido = true;
				releaseNotesIncludes.add(linha);
				// linhas que n??o sejam de include de release continuam onde est??o, espa??os em
				// branco permanessem onde est??o se n??o estiverem entre os includes de releases
			} else if (StringUtils.isNotBlank(linha) || !encontrouAlgumIncludeValido) {
				linhasArquivoAlterado.add(linha);
			}
		}
		messages.info("Existem " + releaseNotesIncludes.size() + " releases.");
		// 3. ordena os release notes pelo n??mero da vers??o ASC
		Collections.sort(releaseNotesIncludes, new SortReleaseNotesHtmlByVersion());
		// 4. remonta o arquivo:
		for (String releaseInclude : releaseNotesIncludes) {
			// 4.2. Grava cada include e adiciona um \n no in??cio e outro no final
			linhasArquivoAlterado.add("\n" + releaseInclude);
		}
		return String.join("\n", linhasArquivoAlterado);
	}

	class SortReleaseNotesHtmlByVersion implements Comparator<String> {
		public int compare(String a, String b) {
			String versionAStr = getVersionFromReleaseNotesInclude(a, JiraService.RELEASE_NOTES_FILENAME_PREFIX, JiraService.FILENAME_SUFFIX_HTML);
			List<Integer> versionA = Utils.getVersionFromString(versionAStr, "-|\\.");
			String versionBStr = getVersionFromReleaseNotesInclude(b, JiraService.RELEASE_NOTES_FILENAME_PREFIX, JiraService.FILENAME_SUFFIX_HTML);
			List<Integer> versionB = Utils.getVersionFromString(versionBStr, "-|\\.");

			return Utils.compareVersionsDesc(versionA, versionB);
		}
	}

	private String getVersionFromReleaseNotesInclude(String include, String prefix, String suffix) {
		return include.replaceFirst(".*" + prefix, "").replaceFirst(suffix + ".*", "");
	}

	private String translateCategoryToPathName(String categoryName) {
		String pathName = null;
		if(StringUtils.isNotBlank(categoryName)) {
			String catNameNormalized = StringUtils.stripAccents(categoryName.toLowerCase());
			pathName = catNameNormalized.replaceAll(" ", "-");
		}

		return pathName;
	}

	private String translateSubCategoryToPathName(String subCategoryName, String gitlabProjectId) {
		String fullPath = null;
		// recupera o arquivo index.adoc, localiza o nome da subcategoria e com isso identifica o pathname
		String filePath = PATHNAME_SRC_DOCUMENTACAO + "index.adoc";
		String conteudoArquivoIndex = gitlabService.getRawFileFromDefaultBranch(gitlabProjectId, filePath);

		if(StringUtils.isNotBlank(conteudoArquivoIndex)) {
			String[] linhas = conteudoArquivoIndex.split("\n");
			for (String linha : linhas) {
				if(StringUtils.isNotBlank(subCategoryName) && linha.contains(subCategoryName)) {
					fullPath = Utils.getPathFromAsciidocLink(linha);
					break;
				}
			}
		}
		return fullPath;
	}
}