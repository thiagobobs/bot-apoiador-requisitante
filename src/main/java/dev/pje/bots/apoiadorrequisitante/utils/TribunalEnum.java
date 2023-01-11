package dev.pje.bots.apoiadorrequisitante.utils;

public enum TribunalEnum {

	// Não se aplica ou todos os tribunais
	NA("000", "Não se aplica", "NA"),
	// STJ, STF e CNJ
	STF("100", "Supremo Tribunal Federal", "STF"),
	CNJ("200", "Conselho Nacional de Justiça", "CNJ"),
	STJ("300", "Superior Tribunal de Justiça", "STJ"),
	// Justiça Federal
	CJF("490", "Conselho de Justiça Federal", "CJF"),
	TRF1("401", "Tribunal Regional Federal da 1a Região", "TRF1"),
	TRF2("402", "Tribunal Regional Federal da 2a Região", "TRF2"),
	TRF3("403", "Tribunal Regional Federal da 3a Região", "TRF3"),
	TRF4("404", "Tribunal Regional Federal da 4a Região", "TRF4"),
	TRF5("405", "Tribunal Regional Federal da 5a Região", "TRF5"),
    TRF6("406", "Tribunal Regional Federal da 6a Região", "TRF6"),
	// Justiça do trabalho
	TST("500", "Tribunal Superior do Trabalho", "TST"),
	CSJT("590", "Conselho Superior da Justiça do Trabalho", "CSJT"),
	TRT1("501", "Tribunal Regional do Trabalho da 1a Região", "TRT1"),
	TRT2("502", "Tribunal Regional do Trabalho da 2a Região", "TRT2"),
	TRT3("503", "Tribunal Regional do Trabalho da 3a Região", "TRT3"),
	TRT4("504", "Tribunal Regional do Trabalho da 4a Região", "TRT4"),
	TRT5("505", "Tribunal Regional do Trabalho da 5a Região", "TRT5"),
	TRT6("506", "Tribunal Regional do Trabalho da 6a Região", "TRT6"),
	TRT7("507", "Tribunal Regional do Trabalho da 7a Região", "TRT7"),
	TRT8("508", "Tribunal Regional do Trabalho da 8a Região", "TRT8"),
	TRT9("509", "Tribunal Regional do Trabalho da 9a Região", "TRT9"),
	TRT10("510", "Tribunal Regional do Trabalho da 10a Região", "TRT10"),
	TRT11("511", "Tribunal Regional do Trabalho da 11a Região", "TRT11"),
	TRT12("512", "Tribunal Regional do Trabalho da 12a Região", "TRT12"),
	TRT13("513", "Tribunal Regional do Trabalho da 13a Região", "TRT13"),
	TRT14("514", "Tribunal Regional do Trabalho da 14a Região", "TRT14"),
	TRT15("515", "Tribunal Regional do Trabalho da 15a Região", "TRT15"),
	TRT16("516", "Tribunal Regional do Trabalho da 16a Região", "TRT16"),
	TRT17("517", "Tribunal Regional do Trabalho da 17a Região", "TRT17"),
	TRT18("518", "Tribunal Regional do Trabalho da 18a Região", "TRT18"),
	TRT19("519", "Tribunal Regional do Trabalho da 19a Região", "TRT19"),
	TRT20("520", "Tribunal Regional do Trabalho da 20a Região", "TRT20"),
	TRT21("521", "Tribunal Regional do Trabalho da 21a Região", "TRT21"),
	TRT22("522", "Tribunal Regional do Trabalho da 22a Região", "TRT22"),
	TRT23("523", "Tribunal Regional do Trabalho da 23a Região", "TRT23"),
	TRT24("524", "Tribunal Regional do Trabalho da 24a Região", "TRT24"),
	// Justiça Eleitoral
	TSE("600", "Tribunal Superior Eleitoral", "TSE"),
	TREAC("601", "Tribunal Regional Eleitoral do Acre", "TRE-AC"),
	TREAL("602", "Tribunal Regional Eleitoral de Alagoas", "TRE-AL"),
	TREAP("603", "Tribunal Regional Eleitoral do Amapá", "TRE-AP"),
	TREAM("604", "Tribunal Regional Eleitoral do Amazonas", "TRE-AM"),
	TREBA("605", "Tribunal Regional Eleitoral da Bahia", "TRE-BA"),
	TRECE("606", "Tribunal Regional Eleitoral do Ceará", "TRE-CE"),
	TREDF("607", "Tribunal Regional Eleitoral do Distrito Federal", "TRE-DF"),
	TREES("608", "Tribunal Regional Eleitoral do Espírito Santo", "TRE-ES"),
	TREGO("609", "Tribunal Regional Eleitoral de Goiás", "TRE-GO"),
	TREMA("610", "Tribunal Regional Eleitoral do Maranhão", "TRE-MA"),
	TREMT("611", "Tribunal Regional Eleitoral de Mato Grosso", "TRE-MT"),
	TREMS("612", "Tribunal Regional Eleitoral de Mato Grosso do Sul", "TRE-MS"),
	TREMG("613", "Tribunal Regional Eleitoral de Minas Gerais", "TRE-MG"),
	TREPA("614", "Tribunal Regional Eleitoral do Pará", "TRE-PA"),
	TREPB("615", "Tribunal Regional Eleitoral da Paraíba", "TRE-PB"),
	TREPR("616", "Tribunal Regional Eleitoral do Paraná", "TRE-PR"),
	TREPE("617", "Tribunal Regional Eleitoral de Pernambuco", "TRE-PE"),
	TREPI("618", "Tribunal Regional Eleitoral do Piauí", "TRE-PI"),
	TRERJ("619", "Tribunal Regional Eleitoral do Rio de Janeiro", "TRE-RJ"),
	TRERN("620", "Tribunal Regional Eleitoral do Rio Grande do Norte", "TRE-RN"),
	TRERS("621", "Tribunal Regional Eleitoral do Rio Grande do Sul", "TRE-RS"),
	TRERO("622", "Tribunal Regional Eleitoral de Rondônia", "TRE-RO"),
	TRERR("623", "Tribunal Regional Eleitoral de Roraima", "TRE-RR"),
	TRESC("624", "Tribunal Regional Eleitoral de Santa Catarina", "TRE-SC"),
	TRESE("625", "Tribunal Regional Eleitoral de Sergipe", "TRE-SE"),
	TRESP("626", "Tribunal Regional Eleitoral de São Paulo", "TRE-SP"),
	TRETO("627", "Tribunal Regional Eleitoral de Tocantins", "TRE-TO"),
	// Justiça Militar
	STM("700", "Superior Tribunal Militar", "STM"),
	CJM1("701", "1a Circunscrição de Justiça Militar", "CJM1"),
	CJM2("702", "2a Circunscrição de Justiça Militar", "CJM2"),
	CJM3("703", "3a Circunscrição de Justiça Militar", "CJM3"),
	CJM4("704", "4a Circunscrição de Justiça Militar", "CJM4"),
	CJM5("705", "5a Circunscrição de Justiça Militar", "CJM5"),
	CJM6("706", "6a Circunscrição de Justiça Militar", "CJM6"),
	CJM7("707", "7a Circunscrição de Justiça Militar", "CJM7"),
	CJM8("708", "8a Circunscrição de Justiça Militar", "CJM8"),
	CJM9("709", "9a Circunscrição de Justiça Militar", "CJM9"),
	CJM10("710", "10a Circunscrição de Justiça Militar", "CJM10"),
	CJM11("711", "11a Circunscrição de Justiça Militar", "CJM11"),
	CJM12("712", "12a Circunscrição de Justiça Militar", "CJM12"),
	// Justiça dos estados
	TJAC("801", "Tribunal de Justiça do Acre", "TJAC"),
	TJAL("802", "Tribunal de Justiça de Alagoas", "TJAL"),
	TJAP("803", "Tribunal de Justiça do Amapá", "TJAP"),
	TJAM("804", "Tribunal de Justiça do Amazonas", "TJAM"),
	TJBA("805", "Tribunal de Justiça da Bahia", "TJBA"),
	TJCE("806", "Tribunal de Justiça do Ceará", "TJCE"),
	TJDFT("807", "Tribunal de Justiça do Distrito Federal e dos Territórios", "TJDFT"),
	TJES("808", "Tribunal de Justiça do Espírito Santo", "TJES"),
	TJGO("809", "Tribunal de Justiça de Goiás", "TJGO"),
	TJMA("810", "Tribunal de Justiça do Maranhão", "TJMA"),
	TJMT("811", "Tribunal de Justiça de Mato Grosso", "TJMT"),
	TJMS("812", "Tribunal de Justiça de Mato Grosso do Sul", "TJMS"),
	TJMG("813", "Tribunal de Justiça de Minas Gerais", "TJMG"),
	TJPA("814", "Tribunal de Justiça do Pará", "TJPA"),
	TJPB("815", "Tribunal de Justiça da Paraíba", "TJPB"),
	TJPR("816", "Tribunal de Justiça do Paraná", "TJPR"),
	TJPE("817", "Tribunal de Justiça de Pernambuco", "TJPE"),
	TJPI("818", "Tribunal de Justiça do Piauí", "TJPI"),
	TJRJ("819", "Tribunal de Justiça do Rio de Janeiro", "TJRJ"),
	TJRN("820", "Tribunal de Justiça do Rio Grande do Norte", "TJRN"),
	TJRS("821", "Tribunal de Justiça do Rio Grande do Sul", "TJRS"),
	TJRO("822", "Tribunal de Justiça de Rondônia", "TJRO"),
	TJRR("823", "Tribunal de Justiça de Roraima", "TJRR"),
	TJSC("824", "Tribunal de Justiça de Santa Catarina", "TJSC"),
	TJSE("825", "Tribunal de Justiça de Sergipe", "TJSE"),
	TJSP("826", "Tribunal de Justiça de São Paulo", "TJSP"),
	TJTO("827", "Tribunal de Justiça de Tocantins", "TJTO"),
	//Tribunais de Justiça Militar
	TJMMG("913", "Tribunal de Justiça Militar de Minas Gerais", "TJMMG"),
	TJMRS("921", "Tribunal de Justiça Militar do Rio Grande do Sul", "TJMRS"),
	TJMSP("926", "Tribunal de Justiça Militar de São Paulo", "TJMSP");

	private String jtr;
    private String nome;
    private String sigla;

	private TribunalEnum(String jtr, String nome, String sigla) {
		this.jtr = jtr;
		this.nome = nome;
		this.sigla = sigla;
	}

	/**
	 * @return Número JTR do respectivo tribunal
	 */
	public String getJtr() {
		return jtr;
	}

	/**
	 * @return Nome do respectivo tribunal
	 */
    public String getNome() {
		return nome;
	}

    /**
     * @return Sigla do respectivo tribunal
     */
	public String getSigla() {
		return sigla;
	}

	public static TribunalEnum findByJTR(String jtr) {
        for (TribunalEnum enumValue : TribunalEnum.values()) {
            if (enumValue.jtr.equalsIgnoreCase(jtr)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("No result with JTR: " + jtr + " found");
    }
	
	public static TribunalEnum findBySigla(String sigla) {
        for (TribunalEnum enumValue : TribunalEnum.values()) {
            if (enumValue.sigla.equalsIgnoreCase(sigla)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("No result with sigla: " + sigla + " found");
    }
	
}
