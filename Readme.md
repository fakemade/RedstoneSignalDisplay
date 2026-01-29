# Redstone Signal Display

**Redstone Signal Display** is a small and lightweight **PaperMC** plugin that allows players to see the current **Redstone signal strength** in real time.  
It is especially useful for **Bedrock players** playing through GeyserMC.

---

## 🚀 Features

- Displays Redstone signal strength in real time
- Designed for PaperMC servers (aim was **Bedrock** players)
- Lightweight and performance-friendly
- Simple and intuitive usage

---

## 📦 Installation

1. Download the latest `.jar` file from the Releases page or from Hangar.
2. Place it into your server's `plugins/` folder.
3. Restart the server.
4. The plugin will be enabled automatically.

---

## ⚙️ Configuration

Plugin provides a configuration file, it will be generated on first launch.

Configuration:

```yaml
display-radius: 10.0 #length fo visible
display-height: 0.35 #height of text above redstone
vertical-range: 5 #range of searching redstone
text-color: "#FF5555" #color of text
refresh-interval-ticks: 10 #interval for update redstone signal values
