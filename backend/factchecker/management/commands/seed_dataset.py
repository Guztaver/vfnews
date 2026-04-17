from django.core.management.base import BaseCommand
from factchecker.models import DatasetEntry

class Command(BaseCommand):
    help = 'Seeds the dataset with initial electoral context data'

    def handle(self, *args, **kwargs):
        data = [
            # True claims
            ("As eleições de 2022 foram seguras e auditáveis.", "true", "eleição"),
            ("O sistema de votação eletrônico brasileiro é utilizado desde 1996.", "true", "urna, voto"),
            ("A campanha eleitoral oficial começa em agosto.", "true", "campanha"),
            
            # False/Misleading claims
            ("As urnas eletrônicas foram fraudadas em 2018.", "false", "urna, fraude"),
            ("O voto impresso é a única forma de garantir uma eleição sem fraude.", "false", "voto, fraude"),
            ("Houve mais votos do que eleitores em certas cidades.", "false", "voto, fraude"),
            ("Lula foi solto apenas por uma manobra política sem base legal.", "false", "lula, pt"),
            ("Bolsonaro nunca criticou o sistema de urnas antes de 2018.", "false", "bolsonaro, urna"),
        ]

        for text, label, keywords in data:
            entry, created = DatasetEntry.objects.get_or_create(
                text=text,
                defaults={'label': label, 'keywords': keywords}
            )
            if created:
                self.stdout.write(self.style.SUCCESS(f'Created entry: {text[:30]}...'))
            else:
                self.stdout.write(f'Entry already exists: {text[:30]}...')

        self.stdout.write(self.style.SUCCESS('Successfully seeded dataset'))
