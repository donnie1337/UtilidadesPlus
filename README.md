# UtilidadesPlus

Conjunto de utilidades gerais do servidor, reunindo recursos de **MOTD, preferências pessoais, visual, TAB e proteção de comandos**.

## ✨ Funcionalidades

### 🏠 MOTD
- MOTD configurável para entrada dos jogadores.
- Mensagens de entrada e saída configuráveis.
- `/motdplus reload` para recarregar as configurações do MOTD.
- Alias `/motd`.

### ⚙️ Preferências pessoais
- `/configurar` abre a interface de configuração das utilidades pessoais.
- Preferências persistidas por jogador.
- Configurações carregadas novamente durante o reload do plugin.

### 📋 Visual e TAB
- Gerenciamento do visual do texto do servidor.
- Sistema de TAB integrado ao plugin.
- Monitoramento do TPS do servidor.

### 🛡️ Proteção de comandos
- Controle de acesso ao Tab Complete de comandos internos.
- Proteção relacionada aos comandos `/spigot` e `/bukkit`.
- Lista de plugins pode ser protegida por permissão.

### 🚫 Colisão entre jogadores
O plugin mantém a colisão entre jogadores desativada de forma automática, inclusive para jogadores que entram posteriormente no servidor.

## 🎮 Comandos

| Comando | Função |
|---|---|
| `/motdplus reload` | Recarrega a configuração do MOTD. |
| `/motd reload` | Alias do comando de MOTD. |
| `/configurar` | Abre o menu de configuração das utilidades pessoais. |

## 🔑 Permissões

| Permissão | Função | Padrão |
|---|---|---|
| `utilidadesplus.motd.admin` | Recarregar o MOTD | `op` |
| `utilidadesplus.configurar` | Abrir o menu de configuração | `false` |
| `utilidadesplus.plugins` | Visualizar a lista de plugins | `false` |
| `utilidadesplus.admin` | Visualizar Tab Complete de comandos internos | `false` |

## 🔗 Integração

O UtilidadesPlus possui integração com o CargoPlus para respeitar a estrutura de permissões do servidor.

## 🏗️ Plataforma

- Java 26
- Spigot API 26.2
- Maven

## 🧪 Build

```bash
mvn -B clean package
```

O projeto possui workflow de build no GitHub Actions.

> **Nota:** o sistema de cores `/cor` pertence ao **ChatPlus** e não ao UtilidadesPlus.
