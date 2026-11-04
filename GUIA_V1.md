# WiFi VISION — Versão 1.0 (Monitor Wi-Fi)

Guia de estudo + instruções. Tudo foi escrito para você **entender**, não só copiar.

> **Atualização de 03/11/2026:** primeira etapa revisada no Android Studio. As tarefas :app:assembleDebug, :app:lintDebug e :app:testDebugUnitTest foram executadas com sucesso. Não há testes unitários próprios; a tarefa não equivale a testes de comportamento. APK debug em app/build/outputs/apk/debug/app-debug.apk. Testes em celular físico ainda pendentes.
>
> A coleta agora observa redes Wi-Fi por NetworkCallback, independentemente da conexão padrão, com FLAG_INCLUDE_LOCATION_INFO no Android 12+. WifiManager.connectionInfo fica restrito ao Android 8–11 como compatibilidade. O dashboard inclui gateway IPv4, porcentagem estimada e estado de internet não validada. O histórico em memória é limpo em desconexão, leitura indisponível ou mudança de identidade da rede. A coleta pausa sem observadores e não faz varreduras.
>
> O texto histórico abaixo descreve a implementação original; as descrições antigas de connectionInfo e da limitação da rede ativa foram substituídas pela implementação acima.
>
> **Validação física necessária:** Android 8–11 e 12+, conceder apenas localização aproximada e depois precisa; negar e revogar permissão; voltar das configurações; desligar localização e Wi-Fi; Wi-Fi sem internet com dados móveis ativos; VPN; roaming entre BSSIDs; rotação; segundo plano; conferir IP/gateway e link com o roteador; caminhar e verificar RSSI em 2,4/5/6 GHz quando o hardware permitir.
>
> **Próximas etapas:** radar com varreduras limitadas pelo Android; Room e projetos por cômodo com posição manual; planta e escala; interpolação com máscara de confiança; comparação e exportação; teste de velocidade com consentimento e limite de tráfego; diagnósticos baseados nas medições. Essas telas ainda são provisórias e não produzem resultados simulados.

---

## 1. Como abrir, rodar e gerar o APK

**Requisitos:** Android Studio Koala (2024.1) ou mais novo, com JDK 17 (já vem embutido).

1. Descompacte `WiFiVision.zip`.
2. Android Studio → **File → Open** → selecione a pasta `WiFiVision`.
3. Aguarde o **Gradle Sync** (baixa dependências; precisa de internet). Se ele sugerir atualizar o Gradle/AGP, pode aceitar.
4. **Rodar no celular (recomendado, Wi-Fi real):**
   - No celular: *Configurações → Sobre o telefone → toque 7× em "Número da versão"* → ative **Depuração USB** em *Opções do desenvolvedor*.
   - Conecte o cabo, aceite o aviso no celular, escolha o aparelho na barra superior e clique em ▶ **Run**.
   - O emulador também funciona, mas simula um Wi-Fi fixo ("AndroidWifi", sinal -50 dBm): serve para ver a interface, não para testar sinal real.
5. **Gerar APK instalável:**
   - **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
   - Quando terminar, clique em **locate** no aviso. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.
   - Copie para o celular e instale (permita "instalar apps desconhecidos"). O APK *debug* já é instalável e serve para uso pessoal.
   - Para um APK de publicação (assinado): **Build → Generate Signed App Bundle / APK** e crie sua keystore. **Guarde a keystore e a senha**: sem elas você não consegue atualizar o app depois.

---

## 2. Mapa do projeto

```
WiFiVision/
├── settings.gradle.kts            nome do projeto e repositórios
├── build.gradle.kts               versões dos plugins
├── gradle.properties              opções do Gradle (AndroidX ligado)
├── gradle/wrapper/...             versão do Gradle (8.7)
└── app/
    ├── build.gradle.kts           configuração do app e dependências
    └── src/main/
        ├── AndroidManifest.xml    identidade, permissões, Activity
        ├── java/com/codejohn/wifivision/
        │   ├── MainActivity.kt
        │   ├── data/WifiRepository.kt
        │   ├── data/model/WifiSnapshot.kt
        │   ├── util/SignalUtils.kt
        │   └── ui/
        │       ├── PlaceholderFragment.kt
        │       ├── home/HomeFragment.kt
        │       ├── home/HomeViewModel.kt
        │       └── views/SignalGaugeView.kt, SignalChartView.kt
        └── res/  (layouts, cores, textos, ícones, menu)
```

### A arquitetura MVVM em uma frase por camada

| Camada | Arquivo | Papel |
|---|---|---|
| **Model / Data** | `WifiRepository`, `WifiSnapshot` | Lê o Android e entrega dados limpos |
| **ViewModel** | `HomeViewModel` | Guarda o estado e o histórico; sobrevive à rotação |
| **View** | `HomeFragment` + layouts XML | Só desenha o que o ViewModel mandar |

Fluxo dos dados: `WifiRepository` (Flow, a cada 2 s) → `HomeViewModel` (StateFlow) → `HomeFragment` (`render`) → views.

---

## 3. Permissões, uma a uma

| Permissão | Por quê | Observação |
|---|---|---|
| `ACCESS_WIFI_STATE` | Ler RSSI, frequência e velocidade do link | Concedida automaticamente na instalação |
| `ACCESS_NETWORK_STATE` | Saber se a rede ativa é Wi-Fi, se tem internet e qual o IP | Concedida automaticamente |
| `ACCESS_FINE_LOCATION` | O Android só revela **SSID e BSSID** com ela | **Pedida em tempo de execução**, com explicação na tela |

Por que localização para ler nome de Wi-Fi? Porque nomes e MACs de redes ao redor permitem descobrir onde você está. O app **não lê, nem guarda, sua localização**; só precisa da permissão para o Android liberar o nome. Além da permissão, o **serviço de localização do aparelho precisa estar ligado** (o app detecta isso e avisa). O app **nunca** lê senhas de Wi-Fi.

---

## 4. Arquivos de configuração

- **`settings.gradle.kts`**: nome do projeto e de onde baixar bibliotecas (`google()`, `mavenCentral()`).
- **`build.gradle.kts` (raiz)**: declara as versões do Android Gradle Plugin (8.5.2) e do Kotlin (1.9.24).
- **`app/build.gradle.kts`**:
  - `namespace` / `applicationId` = `com.codejohn.wifivision` (identidade do app).
  - `minSdk = 26` (Android 8.0), `targetSdk = compileSdk = 34`.
  - `viewBinding = true`: gera classes como `FragmentHomeBinding` para acessar views sem `findViewById`.
  - Bibliotecas: **core-ktx** (extensões Kotlin), **appcompat** (compatibilidade), **material** (Material Design: botões, navegação inferior), **constraintlayout** (reservada para telas futuras), **fragment-ktx** (`activityViewModels`), **lifecycle-viewmodel-ktx** (`viewModelScope`), **lifecycle-runtime-ktx** (`repeatOnLifecycle`), **kotlinx-coroutines-android** (Coroutines/Flow).
- **`AndroidManifest.xml`**: declara permissões, o tema e a `MainActivity` como tela de entrada (`intent-filter MAIN/LAUNCHER`). `uses-feature wifi required=false` permite instalar em aparelhos sem Wi-Fi. `allowBackup=false` evita copiar dados do app para a nuvem.

---

## 5. Código Kotlin, arquivo por arquivo

### `WifiSnapshot.kt` (modelo)
`data class` imutável com tudo que sabemos do Wi-Fi em um instante. Campos anuláveis (`String?`, `Int?`) representam dados que o Android pode esconder. A UI decide o que mostrar quando vier `null`.

### `SignalUtils.kt` (funções puras)
- `qualityPercent(rssi)`: converte dBm em % estimada: -100 dBm → 0%, -50 dBm → 100%. É uma convenção popular, **não uma medida física exata**.
- `levelOf(rssi)`: Excelente (≥ -55), Boa (≥ -67), Moderada (≥ -75), Fraca (≥ -85), Muito fraca. Cada nível tem texto e cor.
- `bandOf` / `channelOf`: frequência em MHz → banda (2,4/5/6 GHz) e canal (ex.: 5180 MHz → canal 36).
- Por não depender do Android, essas funções são as mais fáceis de cobrir com teste unitário no futuro.

### `WifiRepository.kt` (a única classe que fala com o Android)
- `snapshots(intervalMs)`: um `Flow` que emite um `WifiSnapshot` a cada 2 s. Só roda enquanto alguém coleta. `flowOn(Dispatchers.Default)` tira o trabalho da thread principal.
- `readSnapshot()`:
  1. **ConnectivityManager + NetworkCapabilities**: a rede ativa usa `TRANSPORT_WIFI`? Tem `NET_CAPABILITY_VALIDATED` (internet confirmada)?
  2. **WifiManager.connectionInfo**: RSSI, frequência, velocidade do link, SSID, BSSID. (Marcado como *deprecated* no Android 12, mas funcional; migraremos para `NetworkCallback` com `FLAG_INCLUDE_LOCATION_INFO` quando fizer sentido.)
  3. **LinkProperties**: endereço IPv4 local.
- Valores "falsos" do Android são tratados: `-127` (RSSI inválido), `"<unknown ssid>"` e `02:00:00:00:00:00` viram `null`.
- `applicationContext` evita vazar a Activity.

### `HomeViewModel.kt`
- `uiState: StateFlow<WifiUiState>`: transforma cada snapshot em estado de tela e acumula o histórico (últimos 60 valores de RSSI).
- `SharingStarted.WhileSubscribed(5_000)`: a leitura **pausa** 5 s depois que a tela some e **retoma** ao voltar, o que economiza bateria.

### `HomeFragment.kt`
- `activityViewModels()`: o ViewModel vive na Activity, então o histórico não se perde ao trocar de aba.
- `repeatOnLifecycle(STARTED)`: coleta o `StateFlow` só com a tela visível.
- `render()` divide o trabalho: `renderStatus`, `renderGauge`, `renderIndicators`, `renderInfo`, `renderPermissionCard`.
- `permissionLauncher`: `registerForActivityResult(RequestPermission)` pede a permissão. Se o usuário marcou "não perguntar de novo", abre as configurações do app.
- `onDestroyView` zera `_binding` para evitar vazamento de memória.

### `SignalGaugeView.kt` e `SignalChartView.kt` (views customizadas)
Desenhadas direto no `Canvas`, sem bibliotecas:
- **Gauge:** arco de 270° (`drawArc`), brilho neon feito com um segundo arco mais grosso e translúcido, animação com `ValueAnimator`.
- **Chart:** `Path` para a linha, gradiente (`LinearGradient`) para o preenchimento, eixo fixo de -100 a -30 dBm, valor mais novo à direita.

### `MainActivity.kt` e `PlaceholderFragment.kt`
`MainActivity` hospeda a `BottomNavigationView` e troca fragmentos. As abas Velocidade, Radar, Cobertura e Configurações mostram uma tela provisória ("Disponível na versão X"), que será substituída a cada etapa.

---

## 6. Recursos (res/)

- **`values/colors.xml`**: paleta preto/verde neon/cinza. As cores `signal_*` serão reaproveitadas no mapa de calor da v4.0.
- **`values/themes.xml`**: herda de `Theme.Material3.Dark.NoActionBar` e troca as cores primárias por verde neon e o fundo por preto.
- **`values/strings.xml`**: todos os textos em pt-BR (boa prática: nada de texto fixo no código; facilita traduzir depois).
- **`layout/`**: `activity_main` (container + navegação), `fragment_home` (dashboard), `item_info_row` e `item_indicator` (peças reutilizadas via `<include>`), `fragment_placeholder`.
- **`drawable/`**: ícones vetoriais, fundo dos cartões, ícone do app.
- **`menu/bottom_nav.xml`** e **`color/nav_item_color.xml`**: itens da navegação e cor ativo/inativo.

---

## 7. Roteiro de testes da v1.0

- [ ] App abre com tema escuro e as 5 abas aparecem.
- [ ] **Sem permissão:** aparece o cartão amarelo explicando; SSID/BSSID mostram "Requer permissão"; RSSI, frequência, canal e link já funcionam.
- [ ] Conceda a permissão: o SSID aparece em até 2 s.
- [ ] Desligue a localização do sistema: surge o aviso "Localização do sistema desligada" e o SSID some.
- [ ] Ande pela casa: o medidor e o gráfico reagem; a cor muda de verde para amarelo/laranja/vermelho.
- [ ] Desligue o Wi-Fi: status "DESCONECTADO", medidor zerado.
- [ ] Troque de aba e volte: o histórico do gráfico continua.
- [ ] Gire a tela: nada reinicia.
- [ ] Wi-Fi de 5 GHz próximo ao roteador: frequência/canal condizem com o painel do roteador.

---

## 8. Limitações conhecidas da v1.0 (honestidade técnica)

- Se o celular estiver no Wi-Fi **sem internet** e o Android preferir os dados móveis, a "rede ativa" pode ser a móvel, e o app mostrará "desconectado". Resolveremos com `NetworkCallback` em versão futura.
- O BSSID pode vir oculto em alguns aparelhos mesmo com permissão.
- A "% de qualidade" é estimada a partir do RSSI, não uma medição oficial.
- A velocidade do link é **negociada** entre aparelho e roteador. **Não é a velocidade da internet**; isso é o que o Speed Test da v2.0 vai medir.
- Notificações de atualização do Wi-Fi não são em tempo real: o app consulta a cada 2 s.

---

## 9. Próximo passo: Versão 2.0 (Speed Test)

Quando a v1.0 estiver rodando no seu celular, me diga como foi o teste. Na v2.0 vamos adicionar:
- `INTERNET` permission e cliente HTTP (OkHttp) para download/upload/ping/jitter;
- **Room** (com KSP) para o histórico dos testes;
- a aba Velocidade com gráficos de desempenho.
