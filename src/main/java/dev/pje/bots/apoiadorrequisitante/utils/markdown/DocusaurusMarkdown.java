package dev.pje.bots.apoiadorrequisitante.utils.markdown;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DocusaurusMarkdown implements MarkdownInterface {

	public static final String NAME = MARKDOWN_DOCUSAURUS;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String normal(String text) {
		return text;
	}

	@Override
	public String head1(String text) {
		return newLine() + "# " + text + newLine();
	}

	@Override
	public String head2(String text) {
		return newLine() + "## " + text + newLine();
	}

	@Override
	public String head3(String text) {
		return newLine() + "### " + text + newLine();
	}

	@Override
	public String head4(String text) {
		return newLine() + "#### " + text + newLine();
	}

	@Override
	public String bold(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String italic(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String code(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String code(String text, String language) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String code(String text, String language, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String underline(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String strike(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String citation(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String referUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String highlight(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String quote(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String quote(String text, String author, String reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String block(String text) {
		return block(StringUtils.EMPTY, text);
	}

	@Override
	public String block(String title, String text) {
		return String.format(":::info %s%n%s%n:::%n", title, text);
	}

	@Override
	public String color(String text, String color) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String error(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String info(String text) {
		return info(StringUtils.EMPTY, text);
	}

	public String info(String title, String text) {
		return String.format(":::info %s%n%s%n:::%n", title, text);
	}

	@Override
	public String warning(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String newLine() {
		return "\n";
	}

	@Override
	public String newParagraph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String ruler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String listItem(String text) {
		return "- " + text + newLine();
	}

	@Override
	public String link(String url, String text) {
		return String.format("[%s](<%s>)", text, url);
	}

	@Override
	public String link(String url) {
		return link(url, url);
	}

	@Override
	public String image(String path, String alternativeText, String height, String width, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String image(String path, Map<String, String> options) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String substitution(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String firstPlaceIco() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String secondPlaceIco() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thirdPlaceIco() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String MVPIco() {
		// TODO Auto-generated method stub
		return null;
	}

}
