package dev.pje.bots.apoiadorrequisitante.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devplatform.model.bot.ProcessingMessage;

import dev.pje.bots.apoiadorrequisitante.handlers.docs.Documentation02CreateSolutionHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion020GenerateReleaseCandidateHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion040GenerateReleaseNotesHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion050ProcessReleaseNotesHandler;
import dev.pje.bots.apoiadorrequisitante.handlers.lancamentoversao.LanVersion060FinishReleaseNotesProcessingHandler;

@RestController
public class LancamentoVersaoController {
	
	@Autowired
	private LanVersion020GenerateReleaseCandidateHandler handler020;

	@Autowired
	private LanVersion040GenerateReleaseNotesHandler handler040;
	
	@Autowired
	private LanVersion050ProcessReleaseNotesHandler handler050;
	
	@Autowired
	private LanVersion060FinishReleaseNotesProcessingHandler handler060;
	
	@Autowired
	private Documentation02CreateSolutionHandler handler02;
	
	/** Prévia lançamento versão*/

	@PutMapping(value = "/gitlab-project/{id}/release-branch/{version}/create")
	public List<ProcessingMessage> createReleaseBranch(@PathVariable String id, @PathVariable String version) {
		// 1- CRIA O BRANCH RELEASE (release-[NÚMERO_DA_VERSÃO_QUE_SERÁ_LANÇADA]) A PARTIR DO BRANCH DE DESENVOLVIMENTO
		return this.handler050.createReleaseBranch(id, version);
	}
	
	@PutMapping(value = "/gitlab-project/{id}/development-branch/{nextVersion}/update")
	public List<ProcessingMessage> updateDevelepmentBranch(@PathVariable String id, @PathVariable String nextVersion) {
		// 2- NO BRANCH DE DESENVOLVIMENTO ATUALIZE O NÚMERO DA VERSÃO NO POM.XML E CRIE PASTA DE SCRIPTS SQL
		return this.handler050.updateDevelepmentBranch(id, nextVersion);
	}

	@PutMapping(value = "/gitlab-project/{id}/preview-launch/{version}/comunicate")
	public List<ProcessingMessage> comunicatePreviewLaunch(@PathVariable String id, @PathVariable String version, @RequestParam(required = false) Boolean test) {
		// 3- COMUNICA A CRIAÇÃO DO BRANCH RELEASE
		return this.handler020.comunicatePreviewLaunch(version, test);
	}
	
	/** Lançamento oficial versão*/
	
	@PutMapping(value = "/gitlab-project/{id}/relase-branch/{version}/prepare")
	public List<ProcessingMessage> prepareReleaseBranchForLaunchVersion(@PathVariable String id, @PathVariable String version) {
		// 1- RENOMEIA (CASO SEJA NECESSÁRIO) A PASTA/ARQUIVOS DE SCRIPT SQL 
		return this.handler050.atualizaPastaScripts(id, version).messages;
	}
	
	@PutMapping(value = "/gitlab-project/{id}/release-branch/{version}/merge")
	public List<ProcessingMessage> mergeReleaseBranchToStableBranch(@PathVariable String id, @PathVariable String version) {
		// 2- FAZ O MERGE DO BRANCH RELEASE NO BRANCH MASTER (ATENÇÃO: É NECESSÁRIO FAZER O REBASE MANUALMENTE DO RELEASE BRANCH COM O TARGET BRANCH)
		// 3- EXCLUI O BRANCH RELEASE
		return this.handler050.integraReleaseBranchNoBranchMaster(id, version);
	}
	
	@PutMapping(value = "/gitlab-project/{id}/tag/{version}/create")
	public List<ProcessingMessage> createTagStableBranch(@PathVariable String id, @PathVariable String version) {
		// 4- ATUALIZA O NÚMERO DA VERSÃO NO POM.XML DO BRANCH MASTER PARA A VERSÃO A SER LANÇADA
		// 5- CRIA A TAG
		return this.handler050.criaTag(version, id);
	}
	
	@PutMapping(value = "/gitlab-project/{id}/docs/{version}/update")
	public List<ProcessingMessage> updateDocs(@PathVariable String id, @PathVariable String version) {
		// 6- CRIA O RELEASE NOTES DA TAG NO GITLAB
		// 7- CRIA O RELEASE NOTES NO DOCS.PJE
		return this.handler02.atualizaDocs(id, this.handler040.geraReleaseNotes(id, version));
	}
	
	@PutMapping(value = "/gitlab-project/{id}/official-launch/{version}/comunicate")
	public List<ProcessingMessage> comunicate(@PathVariable String id, @PathVariable String version, @RequestParam(required = false) Boolean test) {
		// 8- COMUNICA O LANÇAMENTO DA VERSÃO
		// 9- LIBERA A VERSÃO DO PROJETO ASSOCIADO NO JIRA
		return this.handler060.comunicateOfficialLaunch(this.handler040.geraReleaseNotes(id, version), test);
	}
	
}
