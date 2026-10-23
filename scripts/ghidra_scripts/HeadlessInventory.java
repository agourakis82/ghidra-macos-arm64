// Headless inventory: functions, imports, and defined strings.
// Usage: -postScript HeadlessInventory.java <output-dir>
//@category Headless

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.DataIterator;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.program.model.symbol.SymbolType;

public class HeadlessInventory extends GhidraScript {

	@Override
	public void run() throws Exception {
		String[] args = getScriptArgs();
		if (args.length < 1 || args[0].isBlank()) {
			println("usage: HeadlessInventory.java <output-dir>");
			return;
		}

		File outDir = new File(args[0]);
		if (!outDir.isDirectory() && !outDir.mkdirs()) {
			println("could not create output dir: " + outDir.getAbsolutePath());
			return;
		}

		String base = sanitize(currentProgram.getName());
		File functions = new File(outDir, base + ".functions.txt");
		File imports = new File(outDir, base + ".imports.txt");
		File strings = new File(outDir, base + ".strings.txt");

		int functionCount = writeFunctions(functions);
		int importCount = writeImports(imports);
		int stringCount = writeStrings(strings);

		println("program=" + currentProgram.getName());
		println("language=" + currentProgram.getLanguageID());
		println("functions=" + functionCount + " -> " + functions.getAbsolutePath());
		println("imports=" + importCount + " -> " + imports.getAbsolutePath());
		println("strings=" + stringCount + " -> " + strings.getAbsolutePath());
	}

	private int writeFunctions(File file) throws Exception {
		int count = 0;
		try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
			FunctionIterator iter = currentProgram.getFunctionManager().getFunctions(true);
			while (iter.hasNext() && !monitor.isCancelled()) {
				Function function = iter.next();
				writer.printf("%s\t%s\t%d%n",
					function.getEntryPoint(),
					function.getName(),
					function.getBody().getNumAddresses());
				count++;
			}
		}
		return count;
	}

	private int writeImports(File file) throws Exception {
		int count = 0;
		try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
			SymbolIterator iter = currentProgram.getSymbolTable().getExternalSymbols();
			while (iter.hasNext() && !monitor.isCancelled()) {
				Symbol symbol = iter.next();
				if (symbol.getSymbolType() != SymbolType.FUNCTION &&
					symbol.getSymbolType() != SymbolType.LABEL) {
					continue;
				}
				String library = symbol.getParentNamespace() == null
					? ""
					: symbol.getParentNamespace().getName();
				writer.printf("%s\t%s%n", library, symbol.getName());
				count++;
			}
		}
		return count;
	}

	private int writeStrings(File file) throws Exception {
		int count = 0;
		try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
			DataIterator iter = currentProgram.getListing().getDefinedData(true);
			while (iter.hasNext() && !monitor.isCancelled()) {
				Data data = iter.next();
				String type = data.getDataType().getName().toLowerCase();
				if (!type.contains("string") && !type.contains("unicode")) {
					continue;
				}
				String value = data.getDefaultValueRepresentation();
				if (value.length() < 4) {
					continue;
				}
				writer.printf("%s\t%s%n", data.getAddress(), value);
				count++;
			}
		}
		return count;
	}

	private static String sanitize(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
