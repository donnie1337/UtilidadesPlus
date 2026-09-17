# UtilidadesPlus

O UtilidadesPlus reúne aquelas funções gerais do servidor que não precisam ficar espalhadas em vários plugins.

Aqui ficam coisas como MOTD, configurações pessoais, TAB, proteção de comandos e outras utilidades que eu quero deixar centralizadas.

## O que tem no plugin

### MOTD

- MOTD configurável.
- Mensagem de entrada configurável.
- Mensagem de saída configurável.
- `/motdplus reload` para recarregar as configurações.
- `/motd` como alias.

### Configurações pessoais

- `/configurar` abre o menu de configurações pessoais.
- Preferências ficam salvas por jogador.
- As configurações podem ser recarregadas sem precisar reiniciar o servidor.

### Visual e TAB

- Controle do visual do servidor.
- Sistema de TAB integrado.
- Monitoramento do TPS do servidor.

### Proteção de comandos

- Controle do Tab Complete de comandos internos.
- Proteção relacionada aos comandos `/spigot` e `/bukkit`.
- Proteção da lista de plugins através de permissão.

### Colisão

A colisão entre jogadores fica desativada automaticamente, inclusive para quem entrar no servidor depois.

## Comandos

| Comando | O que faz |
|---|---|
| `/motdplus reload` | Recarrega a configuração do MOTD. |
| `/motd reload` | Alias do comando de MOTD. |
| `/configurar` | Abre o menu de configurações pessoais. |

## Permissões

| Permissão | O que faz | Padrão |
|---|---|---|
| `utilidadesplus.motd.admin` | Recarregar o MOTD | `op` |
| `utilidadesplus.configurar` | Abrir o menu de configuração | `false` |
| `utilidadesplus.plugins` | Ver a lista de plugins | `false` |
| `utilidadesplus.admin` | Ver o Tab Complete de comandos internos | `false` |

## Integração

O UtilidadesPlus possui integração com o CargoPlus para respeitar a estrutura de permissões do servidor.

## Plataforma

- Java 26
- Spigot API 26.2
- Maven

## Build

```bash
mvn -B clean package
```

O projeto possui build automático pelo GitHub Actions.

## Importante

O sistema `/cor` pertence ao **ChatPlus**. Ele não faz parte do UtilidadesPlus.

## Status

O UtilidadesPlus está em desenvolvimento e serve como uma central de utilidades gerais do meu servidor. A ideia é ir colocando aqui as funções que fazem sentido ficar compartilhadas, sem misturar responsabilidades com os outros plugins.
