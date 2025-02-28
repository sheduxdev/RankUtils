# RankUtils Plugin

## Overview
RankUtils is a lightweight Minecraft plugin that provides useful placeholders for group statistics including total money and group sizes. With RankUtils, you can easily display how much money a specific rank has collectively or how many players belong to a certain rank. The plugin provides real-time updates with optimized performance.

## Dependencies
This plugin requires the following dependencies:
- **Vault** - For economy and permissions integration
- **PlaceholderAPI** - For placeholder functionality
- **A permissions plugin** (such as LuckPerms)
- **An economy plugin** (such as EssentialsX)

## Installation
1. Download the RankUtils plugin JAR file
2. Place the JAR file in your server's `plugins` folder
3. Make sure all dependencies are installed
4. Restart or reload your server
5. The plugin will automatically create any necessary configuration files

## Placeholders
RankUtils adds the following placeholders through PlaceholderAPI:

### Group Money Totals
| Placeholder | Description | Example Output |
|-------------|-------------|---------------|
| `%rankutils_totalmoney_[group]%` | Raw total money value for specified group | `1250000.0` |
| `%rankutils_totalmoney_[group]_formatted%` | Formatted total money (K/M/B format) | `1.2M` |
| `%rankutils_totalmoney_[group]_commas%` | Comma-formatted money value | `1,250,000.00` |
| `%rankutils_totalmoney%` | Raw total money for player's own group | `1250000.0` |
| `%rankutils_totalmoney_formatted%` | Formatted total money for player's own group | `1.2M` |
| `%rankutils_totalmoney_commas%` | Comma-formatted money for player's own group | `1,250,000.00` |

### Group Sizes
| Placeholder | Description | Example Output |
|-------------|-------------|---------------|
| `%rankutils_groupsize_[group]%` | Number of players in specified group | `15` |
| `%rankutils_groupsize%` | Number of players in player's own group | `15` |

### Player's Group
| Placeholder | Description | Example Output |
|-------------|-------------|---------------|
| `%rankutils_playergroup%` | Current player's primary group | `vip` |

Replace `[group]` with your actual group name (e.g., `vip`, `admin`, `donor`, etc.)

## Usage Examples

### Using player's own group:
```
> Your Rank: %rankutils_playergroup%
> Members in your rank: %rankutils_groupsize%
> Total money in your rank: %rankutils_totalmoney_formatted%
```

### In scoreboards:
```
> VIP Members: %rankutils_groupsize_vip%
> VIP Total Balance: %rankutils_totalmoney_vip_formatted%
```

### In chat messages:
```
[Server] There are currently %rankutils_groupsize_vip% VIP members on the server!
[Server] VIP members have a total balance of %rankutils_totalmoney_vip_commas% coins
[Server] Your rank (%rankutils_playergroup%) has %rankutils_groupsize% members
```

### In tablists:
```
-----------------
VIP: %rankutils_groupsize_vip% members
Admin: %rankutils_groupsize_admin% members
Your Rank: %rankutils_playergroup%: %rankutils_groupsize% members
-----------------
```

## Performance Considerations
- RankUtils uses an efficient caching system to minimize server impact
- Group statistics are calculated asynchronously to avoid lag
- Data is automatically refreshed every 5 seconds in the background
- Placeholder results are cached for 5 seconds to optimize performance
- Updates are performed asynchronously to maintain server performance

## Commands and Permissions
This plugin does not currently add any commands or permissions.

## Support
If you encounter any issues or have questions, please create an issue on our GitHub repository or contact us through Discord `sheduxdev`.

## License
RankUtils is licensed under the MIT License. See the LICENSE file for more information.