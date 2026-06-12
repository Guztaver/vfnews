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
        } else {
            log.info(
                "Dataset already has {} entries — skipping CSV/API seed.",
                repository.count()
            );
        }

        // 3. Always supplement with balanced fallback dataset of curated
        //    true/false claims about Brazilian politics (deduplicated).
        //    This runs on every startup to ensure the model has quality
        //    balanced training data regardless of CSV/API content.
        List<DatasetEntry> fallback = buildFallbackDataset();
        List<DatasetEntry> newFallback = new ArrayList<>();
        for (DatasetEntry e : fallback) {
            if (!repository.existsByText(e.getText())) {
                newFallback.add(e);
            }
        }
        if (!newFallback.isEmpty()) {
            repository.saveAll(newFallback);
            log.info("Added {} balanced fallback entries.", newFallback.size());
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
        entries.add(
            e(
                "Bolsonaro tomou uma facada durante a campanha eleitoral de 2018 em Juiz de Fora.",
                "true",
                "bolsonaro, facada, campanha"
            )
        );
        entries.add(
            e(
                "Bolsonaro sofreu um atentado a faca em setembro de 2018 em Juiz de Fora Minas Gerais.",
                "true",
                "bolsonaro, atentado, faca"
            )
        );
        entries.add(
            e(
                "Bolsonaro levou uma facada em 2018 e sobreviveu.",
                "true",
                "bolsonaro, facada, sobreviveu"
            )
        );
        entries.add(
            e(
                "Bolsonaro levou uma facada na campanha de 2018 e foi hospitalizado.",
                "true",
                "bolsonaro, facada, hospitalizado"
            )
        );
        entries.add(
            e(
                "A vacina contra COVID-19 contém chips de controle e rastreamento.",
                "false",
                "vacina, chip, controle"
            )
        );
        entries.add(
            e(
                "As vacinas são seguras e passam por testes rigorosos antes da aprovação.",
                "true",
                "vacina, segurança, saúde"
            )
        );
        entries.add(
            e(
                "As vacinas têm chips de controle implantados pelo governo.",
                "false",
                "vacina, chips, governo"
            )
        );
        entries.add(
            e(
                "Bolsonaro nunca foi presidente do Brasil.",
                "false",
                "bolsonaro, presidente, nunca"
            )
        );
        entries.add(
            e(
                "Lula nunca foi presidente do Brasil.",
                "false",
                "lula, presidente, nunca"
            )
        );
        entries.add(
            e(
                "Dilma Rousseff nunca foi presidente do Brasil.",
                "false",
                "dilma, presidente, nunca"
            )
        );
        entries.add(
            e(
                "O Brasil nunca teve uma mulher presidente.",
                "false",
                "dilma, mulher, presidente"
            )
        );
        entries.add(
            e(
                "Dilma Rousseff foi a primeira mulher presidente do Brasil.",
                "true",
                "dilma, mulher, presidente"
            )
        );
        entries.add(
            e(
                "O voto no Brasil é facultativo e ninguém é obrigado a votar.",
                "false",
                "voto, facultativo, obrigatório"
            )
        );
        entries.add(
            e(
                "A urna eletrônica está conectada à internet e pode ser hackeada remotamente.",
                "false",
                "urna, internet, hack"
            )
        );
        entries.add(
            e(
                "O PT roubou bilhões dos cofres públicos durante seus governos.",
                "false",
                "pt, roubo, cofres"
            )
        );
        entries.add(
            e(
                "O Brasil é uma ditadura comunista desde que Lula assumiu.",
                "false",
                "brasil, ditadura, comunismo"
            )
        );
        entries.add(
            e(
                "O exército brasileiro deveria intervir e fechar o STF e o Congresso.",
                "false",
                "exército, intervenção, stf"
            )
        );
        entries.add(
            e(
                "Lula comprou o sítio de Atibaia com dinheiro desviado da Petrobras.",
                "false",
                "lula, sítio, atibaia"
            )
        );
        entries.add(
            e(
                "A facada em Bolsonaro em 2018 foi uma farsa encenada.",
                "false",
                "bolsonaro, facada, farsa"
            )
        );
        entries.add(
            e(
                "Vacinas causam autismo em crianças.",
                "false",
                "vacina, autismo, saúde"
            )
        );
        entries.add(
            e(
                "O TSE anulou milhões de votos para favorecer Lula em 2022.",
                "false",
                "tse, voto, fraude"
            )
        );
        entries.add(
            e(
                "A ditadura militar no Brasil foi um período de prosperidade sem violações de direitos.",
                "false",
                "ditadura militar, direitos"
            )
        );
        entries.add(
            e(
                "A Terra é plana e a NASA esconde a verdade da população.",
                "false",
                "terra plana, nasa"
            )
        );
        entries.add(
            e(
                "O aquecimento global é uma farsa criada por governos para aumentar impostos.",
                "false",
                "aquecimento global, impostos"
            )
        );
        entries.add(
            e(
                "Cloroquina e ivermectina curam a COVID-19 com eficácia comprovada.",
                "false",
                "cloroquina, ivermectina, covid"
            )
        );
        entries.add(
            e(
                "Não houve corrupção nos governos do PT.",
                "false",
                "corrupção, pt, governo"
            )
        );
        entries.add(
            e(
                "O Brasil é o país com a maior carga tributária do mundo.",
                "false",
                "carga tributária, brasil"
            )
        );
        entries.add(
            e(
                "Lula é o atual presidente do Brasil e foi eleito democraticamente em 2022.",
                "true",
                "lula, presidente, eleito"
            )
        );
        entries.add(
            e(
                "Bolsonaro governou o Brasil por um mandato de 4 anos como presidente.",
                "true",
                "bolsonaro, presidente, mandato"
            )
        );
        entries.add(
            e(
                "O Brasil é uma república federativa presidencialista com três poderes.",
                "true",
                "brasil, república, poderes"
            )
        );
        entries.add(
            e(
                "A vacinação em massa é uma das maiores conquistas da saúde pública mundial.",
                "true",
                "vacinação, saúde pública"
            )
        );
        entries.add(
            e(
                "As urnas eletrônicas brasileiras são auditáveis e nunca tiveram fraude comprovada.",
                "true",
                "urna, auditoria, fraude"
            )
        );

        // ── More FALSE claims (expanded for balance) ─────────────────
        entries.add(e("Lula é comunista.", "false", "lula, comunista"));
        entries.add(e("Lula nunca foi presidente.", "false", "lula, presidente"));
        entries.add(e("Bolsonaro nunca foi presidente do Brasil.", "false", "bolsonaro, presidente, nunca"));
        entries.add(e("Dilma nunca foi presidente.", "false", "dilma, presidente"));
        entries.add(e("A vacina tem chips de controle.", "false", "vacina, chips, controle"));
        entries.add(e("As vacinas causam autismo.", "false", "vacina, autismo"));
        entries.add(e("O TSE fraudou as eleições de 2022.", "false", "tse, fraude, eleição"));
        entries.add(e("As urnas eletrônicas não são confiáveis.", "false", "urna, confiança"));
        entries.add(e("Houve fraude nas eleições presidenciais de 2022.", "false", "fraude, eleição, 2022"));
        entries.add(e("O voto impresso é mais seguro que a urna eletrônica.", "false", "voto impresso, urna"));
        entries.add(e("O STF persegue opositores políticos.", "false", "stf, perseguição"));
        entries.add(e("Alexandre de Moraes é um ditador.", "false", "moraes, ditador"));
        entries.add(e("A China controla as eleições brasileiras.", "false", "china, eleição, controle"));
        entries.add(e("O comunismo será implantado no Brasil.", "false", "comunismo, brasil"));
        entries.add(e("As pesquisas eleitorais são todas falsas.", "false", "pesquisa, eleitoral, falsa"));
        entries.add(e("O PT vai confiscar a poupança dos brasileiros.", "false", "pt, confisco, poupança"));
        entries.add(e("Lula vai fechar o Congresso Nacional.", "false", "lula, congresso, fechar"));
        entries.add(e("O agronegócio vai acabar se Lula governar.", "false", "agronegócio, lula"));
        entries.add(e("A Polícia Federal é controlada pelo PT.", "false", "polícia federal, pt, controle"));
        entries.add(e("O governo Lula liberou todas as drogas no Brasil.", "false", "drogas, lula, governo"));
        entries.add(e("Bolsonaro venceu a eleição de 2022.", "false", "bolsonaro, eleição, 2022"));
        entries.add(e("As Forças Armadas devem intervir no governo.", "false", "forças armadas, intervenção"));
        entries.add(e("O Brasil vai virar uma Venezuela.", "false", "brasil, venezuela"));
        entries.add(e("A urna eletrônica pode ser hackeada.", "false", "urna, hack"));
        entries.add(e("O voto pela internet já foi implementado no Brasil.", "false", "voto, internet"));
        entries.add(e("A vacina contra gripe causa paralisia.", "false", "vacina, gripe, paralisia"));
        entries.add(e("A água com limão cura o câncer.", "false", "limão, câncer, cura"));
        entries.add(e("O aquecimento global não existe.", "false", "aquecimento global"));
        entries.add(e("A Terra é plana.", "false", "terra plana"));
        entries.add(e("O homem nunca pisou na Lua.", "false", "lua, homem"));
        entries.add(e("O PT é um partido comunista.", "false", "pt, comunista"));
        entries.add(e("Lula é analfabeto.", "false", "lula, analfabeto"));
        entries.add(e("Bolsonaro é inocente de todos os crimes.", "false", "bolsonaro, inocente, crimes"));
        entries.add(e("A ditadura militar não houve tortura no Brasil.", "false", "ditadura, tortura"));
        entries.add(e("O nazismo foi um movimento de esquerda.", "false", "nazismo, esquerda"));
        entries.add(e("A escravidão no Brasil foi benéfica para os africanos.", "false", "escravidão, brasil"));
        entries.add(e("Não existe racismo no Brasil.", "false", "racismo, brasil"));
        entries.add(e("A CLT foi abolida no governo Bolsonaro.", "false", "clt, bolsonaro"));
        entries.add(e("O governo distribui kit gay nas escolas.", "false", "kit gay, escolas"));
        entries.add(e("A mamadeira de piroca existe nas escolas.", "false", "mamadeira, escola"));
        entries.add(e("O PT inventou o coronavírus.", "false", "pt, coronavírus"));
        entries.add(e("A COVID-19 não existe.", "false", "covid, existe"));
        entries.add(e("Máscaras não protegem contra vírus.", "false", "máscara, vírus, proteção"));
        entries.add(e("O distanciamento social não funciona.", "false", "distanciamento social"));
        entries.add(e("Beber água quente mata o coronavírus.", "false", "água quente, coronavírus"));
        entries.add(e("O chá de erva-doce cura a COVID.", "false", "chá, cura, covid"));
        entries.add(e("A ivermectina previne a COVID-19.", "false", "ivermectina, prevenção, covid"));
        entries.add(e("A hidroxicloroquina é eficaz contra a COVID.", "false", "hidroxicloroquina, covid"));
        entries.add(e("A vacina altera o DNA humano.", "false", "vacina, dna"));
        entries.add(e("Vacinas contêm mercúrio em nível tóxico.", "false", "vacina, mercúrio"));
        entries.add(e("A vacina causa efeitos colaterais em 50% das pessoas.", "false", "vacina, efeitos colaterais"));
        entries.add(e("A fraude nas urnas foi comprovada pelo Exército.", "false", "fraude, urna, exército"));
        entries.add(e("O código-fonte da urna nunca foi auditado.", "false", "código-fonte, urna, auditoria"));
        entries.add(e("As eleições brasileiras não são auditáveis.", "false", "eleição, auditoria"));
        entries.add(e("O voto eletrônico é facilmente fraudável.", "false", "voto eletrônico, fraude"));
        entries.add(e("O tribunal superior eleitoral é corrupto.", "false", "tse, corrupto"));
        entries.add(e("O presidente pode fechar o STF.", "false", "presidente, stf, fechar"));
        entries.add(e("O STF legisla acima do Congresso.", "false", "stf, congresso, legislar"));
        entries.add(e("A Constituição permite intervenção militar.", "false", "constituição, intervenção militar"));
        entries.add(e("O Brasil é governado por uma quadrilha comunista.", "false", "brasil, quadrilha, comunista"));
        entries.add(e("O PT destruiu a economia brasileira.", "false", "pt, economia"));
        entries.add(e("Lula foi condenado sem provas.", "false", "lula, condenação, provas"));
        entries.add(e("A lava jato foi uma conspiração contra Lula.", "false", "lava jato, conspiração, lula"));
        entries.add(e("Sérgio Moro é um criminoso.", "false", "moro, criminoso"));
        entries.add(e("A operação lava jato não encontrou corrupção.", "false", "lava jato, corrupção"));
        entries.add(e("O mensalão nunca existiu.", "false", "mensalão"));
        entries.add(e("O petrolão foi uma invenção da mídia.", "false", "petrolão, mídia"));
        entries.add(e("Não existe corrupção no governo Lula.", "false", "corrupção, lula"));
        entries.add(e("A Petrobras nunca teve esquema de corrupção.", "false", "petrobras, corrupção"));
        entries.add(e("O orçamento secreto foi criado pelo PT.", "false", "orçamento secreto, pt"));
        entries.add(e("O auxílio emergencial foi desviado pelo PT.", "false", "auxílio emergencial, pt"));
        entries.add(e("A reforma da previdência prejudicou os pobres.", "false", "reforma previdência, pobres"));
        entries.add(e("O salário mínimo nunca aumentou acima da inflação.", "false", "salário mínimo, inflação"));
        entries.add(e("O desemprego no Brasil é o maior do mundo.", "false", "desemprego, brasil"));
        entries.add(e("O Brasil tem a pior educação do mundo.", "false", "educação, brasil"));
        entries.add(e("A saúde pública no Brasil é a pior do mundo.", "false", "saúde pública, brasil"));
        entries.add(e("A violência no Brasil é culpa exclusiva do governo.", "false", "violência, brasil, governo"));
        entries.add(e("O Brasil gasta mais com presídios do que com educação.", "false", "presídio, educação"));
        entries.add(e("Os policiais matam mais no Brasil do que em zona de guerra.", "false", "polícia, violência"));
        entries.add(e("O aborto é legalizado no Brasil.", "false", "aborto, brasil"));
        entries.add(e("A ideologia de gênero é ensinada nas escolas.", "false", "ideologia de gênero, escola"));
        entries.add(e("Crianças brasileiras aprendem a ser gays na escola.", "false", "criança, gay, escola"));
        entries.add(e("O PT quer legalizar a pedofilia.", "false", "pt, pedofilia"));
        entries.add(e("A bancada evangélica quer implantar uma teocracia.", "false", "bancada evangélica, teocracia"));
        entries.add(e("Lula fechou todas as igrejas evangélicas.", "false", "lula, igreja, evangélica"));
        entries.add(e("Bolsonaro acabou com a corrupção no Brasil.", "false", "bolsonaro, corrupção"));
        entries.add(e("Nunca houve corrupção no governo Bolsonaro.", "false", "bolsonaro, corrupção"));
        entries.add(e("O governo Bolsonaro foi o melhor da história.", "false", "bolsonaro, governo, melhor"));
        entries.add(e("A economia brasileira cresceu 10% ao ano com Bolsonaro.", "false", "economia, bolsonaro"));
        entries.add(e("Bolsonaro criou mais empregos que todos os presidentes.", "false", "bolsonaro, empregos"));
        entries.add(e("O Brasil não tem terremotos porque está no centro da placa.", "false", "brasil, terremoto, placa"));
        entries.add(e("A Amazônia produz 20% do oxigênio do mundo.", "false", "amazônia, oxigênio"));
        entries.add(e("A floresta amazônica é o pulmão do mundo.", "false", "amazônia, pulmão"));
        entries.add(e("O Brasil é autossuficiente em trigo.", "false", "brasil, trigo"));
        entries.add(e("A energia solar é mais barata que a hidrelétrica no Brasil.", "false", "energia solar, hidrelétrica"));
        entries.add(e("O etanol polui mais que a gasolina.", "false", "etanol, gasolina"));
        entries.add(e("O pré-sal foi descoberto no governo Lula.", "false", "pré-sal, lula"));
        entries.add(e("O Banco Central imprime dinheiro sem lastro.", "false", "banco central, dinheiro"));
        entries.add(e("O Pix é taxado pelo governo.", "false", "pix, taxa"));
        entries.add(e("O governo confiscou a poupança em 1990.", "false", "governo, poupança, confisco"));
        entries.add(e("A nota de 200 reais foi criada para pagar corrupção.", "false", "nota, corrupção"));
        entries.add(e("O real vai acabar.", "false", "real, moeda"));
        entries.add(e("O dólar vai chegar a 10 reais.", "false", "dólar, real"));
        entries.add(e("O PIX vai substituir completamente o dinheiro físico.", "false", "pix, dinheiro físico"));

        // ── More TRUE claims (expanded for balance) ─────────────────
        entries.add(e("Lula foi eleito presidente em 2002 e reeleito em 2006.", "true", "lula, eleito, 2002"));
        entries.add(e("Lula governou o Brasil por oito anos.", "true", "lula, governo, oito anos"));
        entries.add(e("Bolsonaro foi eleito presidente do Brasil em 2018.", "true", "bolsonaro, eleito, 2018"));
        entries.add(e("Bolsonaro governou de 2019 até 2022.", "true", "bolsonaro, governo, 2022"));
        entries.add(e("Dilma Rousseff foi presidente do Brasil de 2011 a 2016.", "true", "dilma, presidente"));
        entries.add(e("Dilma foi a primeira mulher presidente do Brasil.", "true", "dilma, mulher, primeira"));
        entries.add(e("O voto é obrigatório no Brasil para maiores de 18 anos.", "true", "voto, obrigatório"));
        entries.add(e("A capital do Brasil é Brasília.", "true", "capital, brasília"));
        entries.add(e("O Brasil tem 26 estados e um Distrito Federal.", "true", "estados, distrito federal"));
        entries.add(e("O português é o idioma oficial do Brasil.", "true", "português, idioma"));
        entries.add(e("A moeda do Brasil é o real.", "true", "moeda, real"));
        entries.add(e("A escravidão foi abolida no Brasil em 1888.", "true", "escravidão, 1888"));
        entries.add(e("A República foi proclamada no Brasil em 1889.", "true", "república, 1889"));
        entries.add(e("A Constituição de 1988 restabeleceu a democracia.", "true", "constituição, democracia"));
        entries.add(e("Fernando Henrique Cardoso foi presidente por dois mandatos.", "true", "fernando henrique, mandatos"));
        entries.add(e("O Plano Real foi implementado em 1994.", "true", "plano real, 1994"));
        entries.add(e("O Brasil sediou a Copa do Mundo em 2014.", "true", "copa, mundo, 2014"));
        entries.add(e("O Rio de Janeiro sediou as Olimpíadas de 2016.", "true", "olimpíadas, 2016"));
        entries.add(e("A Amazônia é a maior floresta tropical do mundo.", "true", "amazônia, floresta"));
        entries.add(e("O programa Bolsa Família foi criado em 2003.", "true", "bolsa família, 2003"));
        entries.add(e("O SUS garante saúde gratuita a todos os brasileiros.", "true", "sus, saúde, gratuita"));
        entries.add(e("O Brasil é o maior produtor de café do mundo.", "true", "café, produtor"));
        entries.add(e("O Carnaval é a maior festa popular do Brasil.", "true", "carnaval, festa"));
        entries.add(e("O futebol é o esporte mais popular do Brasil.", "true", "futebol, esporte"));
        entries.add(e("O Brasil ganhou cinco Copas do Mundo de futebol.", "true", "copas, futebol"));
        entries.add(e("O Pix foi criado pelo Banco Central em 2020.", "true", "pix, 2020"));
        entries.add(e("A urna eletrônica foi implantada no Brasil em 1996.", "true", "urna, 1996"));
        entries.add(e("O título de eleitor pode ser obtido a partir dos 16 anos.", "true", "título, eleitor"));
        entries.add(e("A biometria foi adotada nas eleições para aumentar a segurança.", "true", "biometria, segurança"));
        entries.add(e("O PT foi fundado em 1980 por sindicalistas.", "true", "pt, 1980"));
        entries.add(e("O impeachment é um processo legal previsto na Constituição.", "true", "impeachment, legal"));
        entries.add(e("A inflação chegou a 80 por cento ao mês antes do Plano Real.", "true", "inflação, plano real"));
        entries.add(e("A Lava Jato investigou corrupção na Petrobras a partir de 2014.", "true", "lava jato, 2014"));
        entries.add(e("A condenação de Lula na Lava Jato foi anulada pelo STF.", "true", "lula, anulada, stf"));
        entries.add(e("A reforma trabalhista foi aprovada em 2017.", "true", "reforma, 2017"));
        entries.add(e("A pandemia de COVID-19 chegou ao Brasil em 2020.", "true", "covid, 2020"));
        entries.add(e("A vacinação contra COVID-19 começou no Brasil em 2021.", "true", "vacinação, 2021"));
        entries.add(e("A urna eletrônica não se conecta à internet.", "true", "urna, internet"));
        entries.add(e("O voto no Brasil é secreto e garantido por lei.", "true", "voto, secreto"));
        entries.add(e("A totalização dos votos é feita eletronicamente.", "true", "votos, eletrônica"));

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
