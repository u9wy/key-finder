# Key Finder

This program is designed to scan and cross-check wallet addresses against a predefined database of funded wallets. It supports Ethereum (ETH) and Bitcoin (BTC) wallets and provides multi-threaded scanning capabilities.

The program generates private keys using a "pages" concept. Each page contains 100 keys. To find out what page a specific key is on you can do the following page = (key \ 100). 
The result of this calculation will give you a starting or ending range. You can repeat the calculation to find the other end of the range you wish to scan. 
The programme can generate private keys from page 1 to page 1157920892373161954235709850086879078528375642790749043826051631415181614944.

## Features
- **Multi-threaded Scanning:** Utilizes multiple threads to scan wallet addresses concurrently for improved efficiency.
- **Database Support:** Loads wallet data from a specified database, providing flexibility for different cryptocurrency types.
- **Random and Sequential Scanning:** Supports both random and sequential scanning modes based on user preference.
- **Cross-Check Scanned Pages:** Optionally cross-checks scanned pages to avoid redundant scanning and improve performance.
- **Configurable Parameters:** Various parameters such as thread multiplier, scanning direction, and scan range are configurable via command line arguments.
- **Graceful Shutdown:** Implements a shutdown hook for graceful program termination, ensuring proper cleanup.

## Prerequisites
- **Java:** Ensure that you have Java installed on your system to run this program.

## Usage
1. **Compile:** Compile the Java program using your preferred Java compiler.
   ```bash
   javac Main.kt

## Run

Make sure you have data files in tsv format for ethereum or bitcoin wallets in ```database/ethereum``` or ```database/bitcoin```.

[Bitcoin Data](http://addresses.loyce.club/) | [Ethereum Data](https://gz.blockchair.com/ethereum/addresses/)

Execute the compiled program with the required command line arguments.

```bash
java -jar key-finder.jar <scannerType> <threadMultiplier> <isSequential> <ascending> <crossCheckScannedPages> <startPage> <endPage>
```

- `<scannerType>`: Specify the scanner type (e.g., "eth" for Ethereum, "btc" for Bitcoin).
- `<threadMultiplier>`: Set the thread multiplier for concurrent scanning.
- `<isSequential>`: Set to "true" for sequential scanning, "false" for random scanning.
- `<ascending>`: Set to "true" for ascending scanning, "false" for descending scanning. Redundant when isSequential = false
- `<crossCheckScannedPages>`: Set to "true" to cross-check scanned pages and avoid redundancy.
- `<startPage>`: Set the starting page for scanning (BigInteger).
- `<endPage>`: Set the ending page for scanning (BigInteger).

## Example

### Random Ethereum Scanning

```bash
java -jar key-finder.jar eth 1 false false false 10000000000000000000000000000000000000000000000000000000000000000000000000 1157920892373161954235709850086879078528375642790749043826051631415181614944
```

### Sequential Ascending Ethereum Scanning

```bash
java -jar key-finder.jar eth 1 true true false 10000000000000000000000000000000000000000000000000000000000000000000000000 1157920892373161954235709850086879078528375642790749043826051631415181614944
```

### Sequential Descending Ethereum Scanning

```bash
java -jar key-finder.jar eth 1 true false false 10000000000000000000000000000000000000000000000000000000000000000000000000 1157920892373161954235709850086879078528375642790749043826051631415181614944
```

Important Notes
Shutdown: To gracefully shut down the program, use the CTRL + C command. The program will save progress and exit.
Thread Safety: The program is designed to handle multi-threading, ensuring proper synchronization for thread safety.

License
This program is open-source and distributed under the MIT License. Feel free to modify and distribute as needed.
