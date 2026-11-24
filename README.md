# AstRead 📖

Aplikasi pembaca dan pelacak novel / light novel modern yang 100% **offline-first**, dibangun secara native menggunakan **Kotlin**, **Jetpack Compose**, dan **Room SQLite**.

[![Download Latest APK](https://img.shields.io/badge/Download-Latest_APK-E5A93B?style=for-the-badge&logo=android&logoColor=black)](https://github.com/HyuraFE/AstRead-App/releases/latest/download/AstRead.apk)
[![GitHub Releases](https://img.shields.io/badge/All_Releases-GitHub-202436?style=for-the-badge&logo=github)](https://github.com/HyuraFE/AstRead-App/releases)

---

## ✨ Fitur Utama

- 📥 **Import EPUB Otomatis:** Cukup pilih file `.epub` dari penyimpanan HP; judul, penulis, jumlah halaman, dan poster cover akan diekstrak otomatis.
- 🎨 **Cover Poster Light Novel:** Menampilkan cover sampul buku berasio ideal langsung di rak buku dengan visual yang rapi.
- 📖 **Reader Mode:**
  - Pemulihan otomatis ke halaman/bab terakhir yang sedang dibaca (*auto-resume*).
  - Ketuk layar untuk menyembunyikan/menampilkan menu navigasi (HUD).
  - Navigasi bab cepat dengan scrubber bar slider.
  - **Toggle 2 Tema Membaca:** *Midnight Espresso* (gelap lembut) & *Warm Cream Paper* (kertas hangat).
- 🔒 **100% Offline-First:** Tanpa login, tanpa internet, dan tanpa tracking. Data tersimpan aman di database Room lokal perangkat.

---

## 🚀 Download APK

Kamu bisa langsung mengunduh file APK versi terbaru melalui tombol di atas atau link berikut:

👉 [**Unduh AstRead.apk (Latest Release)**](https://github.com/HyuraFE/AstRead-App/releases/latest/download/AstRead.apk)

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.0
- **UI Framework:** Jetpack Compose + Material 3
- **Database:** Room Database (SQLite)
- **Image Loader:** Coil Compose
- **Parser:** Native ZIP & XML Pull Parser (EPUB Dublin Core Metadata)
- **CI/CD:** GitHub Actions Auto Build & Release
