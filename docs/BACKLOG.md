# GlyphON — Backlog local

Prioridades: `P0` crítica, `P1` alta, `P2` média, `P3` baixa.

Estados usados: `concluído`, `bloqueado`, `ideia`, `futuro`.

Este backlog parte da baseline funcional e do mapa técnico atuais. Qualquer alteração no overlay deve manter mudanças funcionais e visuais separadas e ser validada num dispositivo real.

# NOW

Pequenas melhorias seguras após refatoração

- **P0 — bloqueado:** Executar o smoke test completo do overlay num dispositivo Android real e registar modelo, versão do Android, build, resultado e observações em `docs/OVERLAY_BASELINE.md`. Bloqueado até existir dispositivo/testador disponível.
- **P1 — concluído:** Manter documentada a baseline funcional do fluxo ativo: permissão, captura de 3/4/5 glyphs, previews, GO, replay, minimizar/restaurar, MANUAL/AUTO, temas, opacidade, SHOW e persistência.
- **P1 — concluído:** Manter o mapa de responsabilidades, acoplamentos, riscos e ordem recomendada de extração do `OverlayService`.
- **P2 — ideia:** Criar uma checklist curta de regressão por tipo de mudança (visual, preferências, geometria e lifecycle), referenciando o smoke test completo existente.
- **P2 — bloqueado:** Confirmar em dispositivo real o comportamento dos assets PNG e dos tamanhos fixos em diferentes densidades e dimensões de ecrã.
- **P3 — ideia:** Uniformizar a terminologia de produto entre GlyphON e o nome técnico legado GlyphTrainer apenas na documentação, sem renomear package, classes ou identificadores.

# NEXT

Próximas funcionalidades com baixo risco

- **P1 — bloqueado:** Extrair seletores puros de recursos (`glyphLimitIcon`, botão flutuante do limite e botão de opacidade), preservando exatamente os mapeamentos e validando antes/depois. Bloqueado até o smoke test da baseline estar registado.
- **P1 — ideia:** Encapsular leitura e escrita das preferências, preservando chaves, defaults, coerções e versão do layout flutuante.
- **P2 — ideia:** Extrair helpers de estilo visual e factories simples sem alterar dimensões, densidade dos drawables ou ordem de criação dos controlos.
- **P2 — ideia:** Converter cálculos de geometria do grupo flutuante em funções puras com inputs explícitos, mantendo separada a aplicação via `WindowManager`.
- **P2 — bloqueado:** Adicionar testes para seletores de recursos, preferências e geometria depois de cada extração segura. Bloqueado até essas unidades estarem isoladas.
- **P3 — ideia:** Acrescentar um registo manual de compatibilidade por dispositivo para posição flutuante, system bars e persistência.

# LATER

Ideias futuras

- **P1 — futuro:** Definir um modelo explícito de estados do overlay — expandido, minimizado, captura, GO, replay e configuração — antes de mover transições.
- **P2 — futuro:** Avaliar a ligação do modelo puro de reconhecimento de glyphs ao fluxo ativo de captura; hoje existe com testes, mas está desligado da experiência principal.
- **P2 — futuro:** Decidir o destino de PROGRAM, `PasswordActivity`, `ProgramActivity` e `GameActivity`: reativar como produto, manter preservados ou remover num projeto separado.
- **P2 — futuro:** Rever e eventualmente reativar o tutorial guiado apenas após uma decisão de UX e testes da ordem de criação das views.
- **P3 — futuro:** Definir a política de temas premium e contadores de utilização atualmente dormentes antes de expor qualquer restrição.
- **P3 — futuro:** Explorar perfis/configurações guardadas para combinações de modo, escala, tema, opacidade e SHOW.

# TECH DEBT

Dívida técnica identificada

- **P0 — bloqueado:** Reduzir o acoplamento dos eventos e callbacks temporizados do `OverlayService`. Bloqueado até existirem testes/observabilidade suficientes para captura, GO, replay, cancelamento e teardown.
- **P0 — bloqueado:** Isolar a coordenação de captura/GO/replay. Deve ser a última grande extração devido ao risco de callbacks atrasados atuarem sobre estado antigo ou views removidas.
- **P1 — ideia:** Separar persistência do serviço sem quebrar compatibilidade com instalações existentes.
- **P1 — ideia:** Separar cálculo de layout das mutações `WindowManager.addView`, `updateViewLayout` e `removeView`.
- **P1 — bloqueado:** Criar cobertura para perda/revogação de permissão e falhas parciais de criação/remoção das views. Bloqueado pela necessidade de uma estratégia de testes Android apropriada.
- **P2 — ideia:** Isolar o bloco do tutorial mantendo `TUTORIAL_ENABLED = false` e sem alterar a ordem ativa de criação.
- **P2 — ideia:** Inventariar e documentar estado dormente: reconhecimento, PROGRAM, tutorial, premium e telemetria.
- **P3 — futuro:** Avaliar uma migração gradual do package legado `glyphtrainer` para a identidade GlyphON, somente com plano explícito de compatibilidade.

# DO NOT TOUCH

Zonas perigosas do OverlayService

- **P0 — bloqueado:** Não alterar isoladamente as constantes de timing de captura, GO e replay nem os respetivos runnables.
- **P0 — bloqueado:** Não quebrar a cadeia `onCaptureFinished -> GO -> showGlyphSequenceRunnable -> startReplay -> replayStepRunnable`.
- **P0 — bloqueado:** Não alterar flags de touch/focus de `drawParams` separadamente de `enableCapture` e `disableCapture`.
- **P0 — bloqueado:** Não remover ou reordenar cancelamentos de callbacks em `cancelSequencePresentation`, `cancelReplay` e `onDestroy`.
- **P0 — bloqueado:** Não mudar isoladamente lifecycle, permissão do overlay, wrappers do `WindowManager` ou comportamento de `stopSelf()` em falhas existentes.
- **P1 — bloqueado:** Não alterar a ordem interna de `minimizeOverlay`/`restoreOverlay`, nem a distinção MANUAL/AUTO, sem caracterizar todas as transições.
- **P1 — bloqueado:** Não alterar separadamente capture bounds, normalização dos paths, previews, replay targets ou H/V scaling.
- **P1 — bloqueado:** Não mudar a ordem de criação dos botões enquanto os `LayoutParams` forem atribuídos por ordem de inicialização.
- **P1 — bloqueado:** Não alterar geometria, gravidade, deltas de drag e limites das system bars do grupo flutuante como mudanças independentes.
- **P1 — bloqueado:** Não renomear nem redefinir chaves, defaults, coerções ou versão do layout das preferências sem migração explícita.
- **P2 — bloqueado:** Não tornar PROGRAM ou tutorial visíveis acidentalmente; ambos são funcionalidades preservadas mas inativas.
- **P2 — bloqueado:** Não converter o serviço para foreground service sem decisão de produto, pois isso introduz comportamento visível ao utilizador.
