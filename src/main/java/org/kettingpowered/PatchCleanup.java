package org.kettingpowered;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PatchCleanup extends SimpleFileVisitor<Path> {
    Set<String> srcFiles;
    PatchCleanup(Set<String> srcFiles){
        this.srcFiles = srcFiles;
    }
    private static final String imp_prefix = "+import ";
    private static final String prefixImports = "((?<=[<\\(@!{])|(?<=(?<!class|enum|record) ))";
    private static final String postfixImports = "(?=[<> \\.,\\(\\):\\[;])";
    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        final File file = path.toFile();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        Scanner scanner = new Scanner(file);
        
        final List<Tuple<String, Optional<Boolean>>> imports =  reader.lines()
                .filter(line -> line.substring(1).startsWith("import"))
                .map(line -> new Tuple<>(line.substring(1).substring("import ".length()), line.charAt(0)=='+'?Optional.of(true):(line.charAt(0)=='-'?Optional.of(false):(Optional<Boolean>)(Optional)Optional.empty())))
                .map(line -> new Tuple<>(line.t1().substring(0, line.t1().indexOf(';')).trim(), line.t2()))
                .toList();
        final Set<String> removed_imports = getImports(imports, false);
        final Set<String> added_imports_set = getImports(imports, true);
        added_imports_set.removeIf(removed_imports::remove);
        final List<Tuple<Pattern, String>> added_imports = added_imports_set.stream()
                .filter(item -> !removed_imports.contains(item))
                .filter(line -> !line.contains("*"))
                .map(line ->  new Tuple<>(line.substring(line.lastIndexOf('.')+1),line))
                .map(tuple -> {
                    String imp_path = tuple.t2().substring(0, tuple.t2().length()-tuple.t1().length());
                    Pattern pattern = Pattern.compile(String.format("%s(%s)?(%s)%s",prefixImports,imp_path.replace(".","\\."), tuple.t1(), postfixImports));
                    return new Tuple<>(pattern, tuple.t2());
                })
                .toList();
        reader = new BufferedReader(new FileReader(file));
        final AtomicReference<String> head = new AtomicReference<>("");
        final List<Hunk> hunks = new ArrayList<>();
        final AtomicBoolean inImports = new AtomicBoolean(true);
        reader.lines().sequential().forEachOrdered(string -> {
            Hunk hunkr = !hunks.isEmpty()?hunks.get(hunks.size()-1):null;
            int coment = string.indexOf("//");
            if (coment<0) coment = string.length();
            if (!string.startsWith("+++") && !string.startsWith("---")){
                if (string.startsWith("@@")){
                  Hunk hunkn = new Hunk(string);
                  hunks.add(hunkn);
                } else if (string.startsWith("+")){
                    if (string.startsWith("+import ")) {
                        //we should never have a change, before a hunk header
                        hunkr.decrement_modified_line(); 
                    } else if (string.substring(0, coment).trim().equals("+") && inImports.get()) {
                        hunkr.decrement_modified_line();
                    }else if ((string.contains("class ") && !string.contains(".class ")) || string.contains("enum ")) {
                      hunkr.add_content_line(string);  
                    } else {
                        inImports.set(false);
                        for (final Tuple<Pattern, String> imp:added_imports) {
                            string = imp.t1().matcher(string).replaceAll(imp.t2());
                        }
                        hunkr.add_content_line(string);
                    }
                    return;
                } else if (string.startsWith("-import ")) {
                    hunkr.add_content_line(" ".concat(string.substring(1)));
                } else {
                    //I'm too lazy to reverse this check...
                    if (string.charAt(0) == ' ' && (string.startsWith(" import") || string.substring(0,coment).trim().isEmpty())){}
                    else inImports.set(false);
                    hunkr.add_content_line(string);
                    return;
                }
            } else {
                String finalString = string;
                head.getAndUpdate(head1 -> head1.concat(finalString.concat("\n")));
            }
        });
        
        try(FileWriter writer = new FileWriter(file)){
            writer.write(head.get());
            for(Hunk hunk:hunks){
                hunk.recalc_linesChanged();
                writer.write(hunk.toString());
            }
        }
        return super.visitFile(path, attrs);
    }

    private static String getImport(String string) {
        String imp = string.substring(1).substring("import ".length());
        imp = imp.substring(0, imp.indexOf(';'));
        return imp;
    }
    
    private Set<String> getImports(List<Tuple<String, Optional<Boolean>>> imports, boolean added){
        return imports.stream()
                .filter(item -> item.t2().isPresent() && item.t2().get() == added)
                .map(Tuple::t1)
                .flatMap(item -> {
                    if (item.charAt(item.length()-1)!='*') return Stream.of(item);
                    String itemPath = item.substring(0, item.length()-1);
                    return Stream.concat(
                            Stream.of(item),
                            srcFiles.stream()
                                    .filter(srcFile -> srcFile.startsWith(itemPath) && srcFile.substring(itemPath.length()).chars().filter(chr -> chr == '.').count() == 0)
                    );
                })
                .collect(Collectors.toSet());
    }
}
