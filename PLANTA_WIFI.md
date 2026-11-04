# WiFi VISION — planta de cobertura 2D

A navegação ativa contém Início, Velocidade, Cobertura e Configurações.
Mapas geográficos, GPS e radar 3D foram retirados da versão ativa. As implementações anteriores estão em legacy/geographic-and-radar como arquivos de referência, sem compilação. MapLibre e Fused Location Provider não são dependências do APK. As tabelas antigas do Room permanecem para preservar dados existentes.

## Uso
1. Cobertura → Planta / escala → Criar planta. Informe nome, largura e altura totais em metros.
2. Importe uma imagem pelo mesmo menu ou use Editar / régua para desenhar cômodos retangulares e paredes, com dois toques.
3. Escolha Marcar roteador e toque na posição informada por você.
4. Conecte o celular ao Wi-Fi. Autorize a leitura Wi-Fi e, se necessário, ative o serviço de localização exigido pelo Android. O aplicativo não usa GPS.
5. Toque em Iniciar mapeamento. Caminhe até um local e toque na posição correspondente da planta. O RSSI fornecido pelo Android é salvo automaticamente, com SSID, BSSID, frequência, horário e posição manual.
6. Toque em um ponto salvo para consultar seus dBm. Pause para editar ou use Nova sessão para comparar outra coleta. Uma mudança de BSSID interrompe a gravação até iniciar outra sessão.
7. A régua mede a distância em linha reta entre dois toques, conforme a largura/altura configuradas. Não mede passos ou percurso.
8. Pontos / mais permite abrir sessões, remover elementos e exportar PNG ou CSV.

## Calor
Os pontos contornados são medições; o preenchimento é uma estimativa por IDW. Exige pelo menos três posições vizinhas em até 2,5 m e fica dentro do contorno medido. Cômodos definidos sem amostras suficientes e vizinhos separados por paredes desenhadas ficam sem interpolação. Cinza significa desconhecido, não ausência confirmada de sinal. A máscara de paredes não é um modelo físico de propagação.

RSSI e identificadores podem ser ocultados pelo Android. Leituras indisponíveis ou antigas não são gravadas. O Android pode manter cache da última leitura de RSSI.

## Velocidade
Mantido o teste HTTPS real de download, upload e latência HTTP/jitter, com consentimento e até 26 MB mais protocolo. Latência HTTP não é ping ICMP nem garantia da capacidade máxima do plano.

## Validação física necessária
- Permissões precisa/aproximada, negação, revogação e localização do sistema desligada.
- RSSI ao caminhar por cômodos, desconexão e troca de BSSID.
- Conferir escala com uma distância conhecida e registrar posições manualmente.
- Importar uma imagem do provedor de arquivos do aparelho e reabrir a planta.
- Conferir persistência após fechar/reabrir e compartilhar os arquivos exportados.
- Teste de velocidade no Wi-Fi residencial, cancelamento e indisponibilidade do servidor.
O RSSI e qualquer localização fornecidos pelo emulador não comprovam a cobertura da residência.
