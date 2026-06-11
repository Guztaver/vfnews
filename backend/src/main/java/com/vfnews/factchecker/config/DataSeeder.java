package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.MLService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DatasetEntryRepository repository;
    private final DatasetSeederService datasetSeederService;
    private final ConsolidadoImporterService consolidadoImporterService;
    private final MLService mlService;

    @Override
    public void run(String... args) throws Exception {
        int removed = datasetSeederService.cleanUntrustedEntries();

        if (removed > 0) {
            new java.io.File("model.ser").delete();
            new java.io.File("vocab.ser").delete();
        }

        if (repository.count() == 0) {
            log.info("Dataset empty — seeding...");

            // 1. Import VFNews Dataset (consolidated claims in Portuguese)
            int consolidadoCount = consolidadoImporterService.importFromCSV();
            log.info("VFNews Dataset import added {} entries.", consolidadoCount);

            // 2. Supplement with Google Fact Check API data
            List<DatasetEntry> apiEntries = datasetSeederService.seedFromApi();
            log.info("API returned {} entries.", apiEntries.size());

            // 3. Add fallback entries only if still under threshold
            if (repository.count() < 100) {
                log.info("Low dataset count — adding fallback dataset.");
                repository.saveAll(buildFallbackDataset());
            }

            log.info("Dataset seeded: {} entries total.", repository.count());
        } else {
            log.info(
                "Dataset already has {} entries — skipping seed.",
                repository.count()
            );
        }

        log.info("Training ML model...");
        mlService.train();
        log.info("ML model trained. Metrics: {}", mlService.getMetrics());
    }

    /**
     * Builds a balanced dataset of true and false claims about Brazilian politics.
     * TRUE claims are verified facts; FALSE claims are common misinformation.
     */
    private List<DatasetEntry> buildFallbackDataset() {
        List<DatasetEntry> entries = new ArrayList<>();

        // ── TRUE claims (verified facts) ──────────────────────────────
        entries.add(
            e(
                "Lula é o atual presidente do Brasil, eleito em 2022.",
                "true",
                "lula, presidente, eleição"
            )
        );
        entries.add(
            e(
                "Lula já foi presidente do Brasil por dois mandatos, de 2003 a 2010.",
                "true",
                "lula, presidente"
            )
        );
        entries.add(
            e(
                "Lula venceu as eleições presidenciais de 2022 contra Jair Bolsonaro.",
                "true",
                "lula, eleição, bolsonaro"
            )
        );
        entries.add(
            e(
                "Lula foi preso em 2018 após condenação na Operação Lava Jato, e solto em 2019.",
                "true",
                "lula, prisão, lava jato"
            )
        );
        entries.add(
            e(
                "Jair Bolsonaro foi presidente do Brasil de 2019 a 2022.",
                "true",
                "bolsonaro, presidente"
            )
        );
        entries.add(
            e(
                "Bolsonaro perdeu a reeleição em 2022 para Lula.",
                "true",
                "bolsonaro, eleição, lula"
            )
        );

        entries.add(
            e(
                "Bolsonaro já foi preso em 1986 por envolvimento em atos de indisciplina militar quando era capitão do Exército.",
                "true",
                "bolsonaro, prisão, exército"
            )
        );
        entries.add(
            e(
                "Bolsonaro foi expulso do Exército por ato terrorista.",
                "false",
                "bolsonaro, expulso, exército, terrorista"
            )
        );
        entries.add(
            e(
                "As eleições de 2022 foram consideradas seguras e auditáveis pelo TSE e observadores internacionais.",
                "true",
                "eleição, tse, segurança"
            )
        );
        entries.add(
            e(
                "O sistema de votação eletrônico brasileiro é utilizado desde 1996 sem casos comprovados de fraude.",
                "true",
                "urna eletrônica, voto, fraude"
            )
        );
        entries.add(
            e(
                "O Tribunal Superior Eleitoral (TSE) é o órgão responsável por organizar as eleições no Brasil.",
                "true",
                "tse, eleição"
            )
        );
        entries.add(
            e(
                "O voto é obrigatório no Brasil para cidadãos entre 18 e 70 anos.",
                "true",
                "voto, obrigatório"
            )
        );
        entries.add(
            e(
                "A urna eletrônica brasileira não é conectada à internet durante a votação.",
                "true",
                "urna, internet, segurança"
            )
        );
        entries.add(
            e(
                "O Brasil adotou o voto eletrônico em todo o território nacional a partir das eleições de 2000.",
                "true",
                "voto eletrônico, urna"
            )
        );
        entries.add(
            e(
                "As eleições presidenciais no Brasil ocorrem a cada 4 anos.",
                "true",
                "eleição, calendário"
            )
        );
        entries.add(
            e(
                "O segundo turno das eleições ocorre quando nenhum candidato atinge 50% dos votos válidos no primeiro turno.",
                "true",
                "segundo turno, eleição"
            )
        );
        entries.add(
            e(
                "A campanha eleitoral oficial no Brasil começa em agosto do ano da eleição.",
                "true",
                "campanha, eleição"
            )
        );
        entries.add(
            e(
                "O Partido dos Trabalhadores (PT) foi fundado em 1980 e já elegeu dois presidentes: Lula e Dilma Rousseff.",
                "true",
                "pt, lula, dilma"
            )
        );
        entries.add(
            e(
                "A Constituição Federal de 1988 é a lei máxima do Brasil e garante direitos fundamentais aos cidadãos.",
                "true",
                "constituição, direitos"
            )
        );
        entries.add(
            e(
                "O Congresso Nacional brasileiro é composto pela Câmara dos Deputados e pelo Senado Federal.",
                "true",
                "congresso, câmara, senado"
            )
        );
        entries.add(
            e(
                "O Supremo Tribunal Federal (STF) é a mais alta instância do poder judiciário brasileiro.",
                "true",
                "stf, judiciário"
            )
        );
        entries.add(
            e(
                "Alexandre de Moraes é ministro do Supremo Tribunal Federal e foi presidente do TSE durante as eleições de 2022.",
                "true",
                "moraes, stf, tse"
            )
        );
        entries.add(
            e(
                "O Brasil possui mais de 150 milhões de eleitores aptos a votar.",
                "true",
                "eleitores, voto"
            )
        );
        entries.add(
            e(
                "Dilma Rousseff sofreu impeachment em 2016 e foi afastada da presidência.",
                "true",
                "dilma, impeachment"
            )
        );
        entries.add(
            e(
                "Michel Temer assumiu a presidência do Brasil após o impeachment de Dilma Rousseff em 2016.",
                "true",
                "temer, presidência, dilma"
            )
        );
        entries.add(
            e(
                "A Operação Lava Jato foi uma investigação da Polícia Federal que apurou esquemas de corrupção envolvendo a Petrobras e políticos.",
                "true",
                "lava jato, corrupção, petrobras"
            )
        );
        entries.add(
            e(
                "O fundo eleitoral é um recurso público destinado ao financiamento das campanhas políticas no Brasil.",
                "true",
                "fundo eleitoral, campanha"
            )
        );
        entries.add(
            e(
                "A Justiça Eleitoral brasileira é composta pelo TSE e pelos Tribunais Regionais Eleitorais (TREs).",
                "true",
                "justiça eleitoral, tse, tre"
            )
        );
        entries.add(
            e(
                "O título de eleitor é o documento que habilita o cidadão brasileiro a votar.",
                "true",
                "título de eleitor, voto"
            )
        );
        entries.add(
            e(
                "A biometria é utilizada para identificação do eleitor nas urnas eletrônicas desde 2008.",
                "true",
                "biometria, urna, eleitor"
            )
        );
        entries.add(
            e(
                "Fernando Haddad foi candidato do PT à presidência em 2018, sendo derrotado por Jair Bolsonaro.",
                "true",
                "haddad, pt, bolsonaro, eleição 2018"
            )
        );
        entries.add(
            e(
                "O voto feminino no Brasil foi instituído em 1932 durante o governo de Getúlio Vargas.",
                "true",
                "voto feminino, história"
            )
        );

        // ── FALSE claims (common misinformation) ──────────────────────
        entries.add(
            e(
                "As urnas eletrônicas foram fraudadas nas eleições de 2018 e 2022.",
                "false",
                "urna, fraude, eleição"
            )
        );
        entries.add(
            e(
                "O voto impresso é a única forma de garantir uma eleição sem fraude.",
                "false",
                "voto impresso, fraude"
            )
        );
        entries.add(
            e(
                "Houve mais votos do que eleitores cadastrados em diversas cidades brasileiras.",
                "false",
                "votos, eleitores, fraude"
            )
        );
        entries.add(
            e(
                "Lula foi solto da prisão apenas por uma manobra política sem base legal.",
                "false",
                "lula, prisão, manobra"
            )
        );
        entries.add(
            e(
                "Bolsonaro nunca criticou o sistema de urnas eletrônicas antes das eleições de 2018.",
                "false",
                "bolsonaro, urna"
            )
        );
        entries.add(
            e(
                "O TSE permitiu fraude nas eleições ao não auditar as urnas eletrônicas.",
                "false",
                "tse, fraude, auditoria"
            )
        );
        entries.add(
            e(
                "O código-fonte das urnas eletrônicas nunca foi aberto para auditoria.",
                "false",
                "código-fonte, urna, auditoria"
            )
        );
        entries.add(
            e(
                "As Forças Armadas encontraram indícios de fraude nas eleições de 2022.",
                "false",
                "forças armadas, fraude, eleição 2022"
            )
        );
        entries.add(
            e(
                "A vacina contra COVID-19 foi usada para implantar chips de controle na população.",
                "false",
                "vacina, chip, covid"
            )
        );
        entries.add(
            e(
                "O PT criou o orçamento secreto para comprar apoio político no Congresso.",
                "false",
                "pt, orçamento secreto"
            )
        );
        entries.add(
            e(
                "Lula pretende fechar igrejas e perseguir cristãos se for eleito.",
                "false",
                "lula, igrejas, perseguição"
            )
        );
        entries.add(
            e(
                "As eleições de 2014 foram fraudadas e Dilma não venceu legitimamente.",
                "false",
                "eleição 2014, fraude, dilma"
            )
        );
        entries.add(
            e(
                "O STF age como uma ditadura e persegue opositores políticos.",
                "false",
                "stf, ditadura, perseguição"
            )
        );
        entries.add(
            e(
                "Alexandre de Moraes mandou prender pessoas sem ordem judicial.",
                "false",
                "moraes, prisão, stf"
            )
        );
        entries.add(
            e(
                "A China controla as urnas eletrônicas brasileiras remotamente.",
                "false",
                "china, urna, controle"
            )
        );
        entries.add(
            e(
                "O sistema eleitoral brasileiro foi hackeado nas eleições de 2018.",
                "false",
                "hackeado, eleição 2018, sistema"
            )
        );
        entries.add(
            e(
                "Bolsonaro venceu as eleições de 2022 mas o resultado foi alterado pelo TSE.",
                "false",
                "bolsonaro, tse, eleição 2022"
            )
        );
        entries.add(
            e(
                "A vacina da COVID-19 causa infertilidade em mulheres.",
                "false",
                "vacina, covid, infertilidade"
            )
        );
        entries.add(
            e(
                "O comunismo será implantado no Brasil se o PT vencer as eleições.",
                "false",
                "comunismo, pt, eleição"
            )
        );
        entries.add(
            e(
                "Todas as pesquisas eleitorais são manipuladas para favorecer a esquerda.",
                "false",
                "pesquisas eleitorais, manipulação"
            )
        );
        entries.add(
            e(
                "Lula é comunista e pretende implantar o regime cubano no Brasil.",
                "false",
                "lula, comunista, cuba"
            )
        );
        entries.add(
            e(
                "A urna eletrônica imprime um comprovante do voto que fica com o eleitor.",
                "false",
                "urna, comprovante, voto"
            )
        );
        entries.add(
            e(
                "O voto no Brasil não é secreto, o governo sabe em quem cada pessoa votou.",
                "false",
                "voto secreto, governo"
            )
        );
        entries.add(
            e(
                "Estrangeiros ilegais votam em massa nas eleições brasileiras.",
                "false",
                "estrangeiros, voto, eleição"
            )
        );
        entries.add(
            e(
                "A Venezuela auditou e aprovou o sistema eleitoral brasileiro.",
                "false",
                "venezuela, auditoria, eleição"
            )
        );

        return entries;
    }

    private static DatasetEntry e(String text, String label, String keywords) {
        return DatasetEntry.builder()
            .text(text)
            .label(label)
            .keywords(keywords)
            .publisher("TSE")
            .build();
    }
}
