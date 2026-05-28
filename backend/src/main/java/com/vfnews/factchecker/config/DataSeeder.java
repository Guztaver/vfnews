package com.vfnews.factchecker.config;

import com.vfnews.factchecker.domain.DatasetEntry;
import com.vfnews.factchecker.repository.DatasetEntryRepository;
import com.vfnews.factchecker.service.MLService;
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
    private final MLService mlService;

    @Override
    public void run(String... args) throws Exception {
        // Clean old entries that lack publisher info (pre-whitelist data)
        int removed = datasetSeederService.cleanUntrustedEntries();

        if (repository.count() == 0) {
            log.info("Dataset empty — seeding...");

            // Try fetching from Google Fact Check API (filtered by trusted publishers)
            List<DatasetEntry> apiEntries = datasetSeederService.seedFromApi();

            if (apiEntries.isEmpty()) {
                log.info(
                    "No API results (key missing?) — using hardcoded fallback dataset."
                );
                List<DatasetEntry> fallback = List.of(
                    DatasetEntry.builder()
                        .text("As eleicoes de 2022 foram seguras e auditaveis.")
                        .label("true")
                        .keywords("eleicao")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text(
                            "O sistema de votacao eletronico brasileiro e utilizado desde 1996."
                        )
                        .label("true")
                        .keywords("urna, voto")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text("A campanha eleitoral oficial comeca em agosto.")
                        .label("true")
                        .keywords("campanha")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text("As urnas eletronicas foram fraudadas em 2018.")
                        .label("false")
                        .keywords("urna, fraude")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text(
                            "O voto impresso e a unica forma de garantir uma eleicao sem fraude."
                        )
                        .label("false")
                        .keywords("voto, fraude")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text(
                            "Houve mais votos do que eleitores em certas cidades."
                        )
                        .label("false")
                        .keywords("voto, fraude")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text(
                            "Lula foi solto apenas por uma manobra politica sem base legal."
                        )
                        .label("false")
                        .keywords("lula, pt")
                        .publisher("TSE")
                        .build(),
                    DatasetEntry.builder()
                        .text(
                            "Bolsonaro nunca criticou o sistema de urnas antes de 2018."
                        )
                        .label("false")
                        .keywords("bolsonaro, urna")
                        .publisher("TSE")
                        .build()
                );
                repository.saveAll(fallback);
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
}
