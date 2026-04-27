package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.MLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DatasetEntryRepository repository;
    private final MLService mlService;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            log.info("Seeding initial dataset...");
            List<DatasetEntry> initialData = List.of(
                    // True claims
                    DatasetEntry.builder().text("As eleições de 2022 foram seguras e auditáveis.").label("true").keywords("eleição").build(),
                    DatasetEntry.builder().text("O sistema de votação eletrônico brasileiro é utilizado desde 1996.").label("true").keywords("urna, voto").build(),
                    DatasetEntry.builder().text("A campanha eleitoral oficial começa em agosto.").label("true").keywords("campanha").build(),

                    // False/Misleading claims
                    DatasetEntry.builder().text("As urnas eletrônicas foram fraudadas em 2018.").label("false").keywords("urna, fraude").build(),
                    DatasetEntry.builder().text("O voto impresso é a única forma de garantir uma eleição sem fraude.").label("false").keywords("voto, fraude").build(),
                    DatasetEntry.builder().text("Houve mais votos do que eleitores em certas cidades.").label("false").keywords("voto, fraude").build(),
                    DatasetEntry.builder().text("Lula foi solto apenas por uma manobra política sem base legal.").label("false").keywords("lula, pt").build(),
                    DatasetEntry.builder().text("Bolsonaro nunca criticou o sistema de urnas antes de 2018.").label("false").keywords("bolsonaro, urna").build()
            );

            repository.saveAll(initialData);
            log.info("Dataset seeded successfully.");
            
            log.info("Training ML model...");
            mlService.train();
            log.info("ML model trained successfully.");
        }
    }
}
