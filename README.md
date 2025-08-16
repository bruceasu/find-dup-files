# Find Dup Files (fdf)

A simple CLI tool to **index, detect duplicate files, and manage file metadata** in a local database.
Useful for cleaning up disk space, finding redundant files, or comparing file indexes.

---

## ✨ Features

* ⚡ **Fast file indexing** → Scan large directories into a lightweight database
* 🔍 **Duplicate detection** → Find duplicate files by content
* 📊 **Difference check** → Compare two indexes and export results
* 🗄 **DB operations** → List, drop, compact, and clean database tables
* 📥 **CSV import/export** → Save and restore indexes
* 🖥 **Cross-platform** → Works on Windows, Linux, macOS
* 🛡 **Lightweight** → No external DB server required (embedded H2 database)

---

## 🚀 Usage

Run the tool with:

```bash
fdf <command> [params]
```

Available commands:

* `index` → Create or update a file index
* `dup` → Find duplicated files
* `diff` → Find differences between two tables
* `db` → Database operations

---

## 🔧 Commands & Options

### 1. Index

Create and manage the index.

```bash
fdf index [options] DIR1 DIR2 ...
```

Options:

| Option | Description                          |
| ------ | ------------------------------------ |
| `-h`   | Show help                            |
| `-v`   | Show verbose log                     |
| `-r`   | Remove non-existing items from table |
| `-t`   | Specify table name                   |
| `-e`   | Exclude paths (comma-separated)      |
| `-i`   | Import from CSV file                 |
| `-x`   | Export to CSV file                   |

---

### 2. Dup

Find duplicated files in the index.

```bash
fdf dup [options]
```

Options:

| Option | Description             |
| ------ | ----------------------- |
| `-h`   | Show help               |
| `-v`   | Verbose log             |
| `-t`   | Table name              |
| `-o`   | Output file for results |

---

### 3. Diff

Compare differences between two tables.

```bash
fdf diff [options]
```

Options:

| Option | Description             |
| ------ | ----------------------- |
| `-h`   | Show help               |
| `-v`   | Verbose log             |
| `-a`   | Left table name         |
| `-b`   | Right table name        |
| `-o`   | Output file for results |

---

### 4. DB

Low-level database operations.

```bash
fdf db [options]
```

Options:

| Option | Description                |
| ------ | -------------------------- |
| `-h`   | Show help                  |
| `-l`   | List tables                |
| `-s`   | Show table items           |
| `-d`   | Drop table                 |
| `-p`   | File path                  |
| `-t`   | Table name                 |
| `-x`   | Treat file path as pattern |
| `-v`   | Verbose log                |
| `-c`   | Clean duplicate file index |
| `-C`   | Compact database           |

---

## 📦 Examples

Index all files under `D:\projects`, excluding `node_modules`, into table `myfiles`:

```bash
fdf index -t myfiles -e node_modules -v D:\projects
```

Find duplicates and export results to CSV:

```bash
fdf dup -t myfiles -o duplicates.csv
```

Compare two file indexes:

```bash
fdf diff -a table_old -b table_new -o diff.csv
```

List all tables in the DB:

```bash
fdf db -l
```

Compact the database:

```bash
fdf db -C
```

---

## 🛠 Build & Run

If using Maven:

```bash
mvn clean package
java -jar target/fdf.jar index -t files
```

---

## 📄 License

This project is licensed under the **Apache License 2.0**.
