/*
 * Copyright (c) 2018.
 * This code is released under The 3-Clause BSD License.
 * https://github.com/techpavan
 */

package com.github.techpavan.maven;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import lombok.Data;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.List;

@Data
public class ArgData {

    @Parameter(names = {"--path", "-p"}, description = "Path to local Maven repository if not in <user_dir>/.m2/repository.")
    private String m2Path;

    @Parameter(names = {"--downloadedBefore", "-db"}, description = "Delete all libraries (even if latest version) downloaded on or before this date (MM-DD-YYYY).", converter = DateToMillisConverter.class)
    private Long downloadedBefore = 0L;

    @Parameter(names = {"--downloadedAfter", "-da"}, description = "Delete all libraries (even if latest version) downloaded on or after this date (MM-DD-YYYY).", converter = DateToMillisConverter.class)
    private Long downloadedAfter = Long.MAX_VALUE;

    @Parameter(names = {"--accessedBefore", "-ab"}, description = "Delete all libraries (even if latest version) last accessed on or before this date (MM-DD-YYYY).", converter = DateToMillisConverter.class)
    private Long accessedBefore = 0L;

    @Parameter(names = {"--accessedAfter", "-aa"}, description = "Delete all libraries (even if latest version) last accessed on or after this date (MM-DD-YYYY).", converter = DateToMillisConverter.class)
    private Long accessedAfter = Long.MAX_VALUE;

    @Parameter(names = {"--ignoreArtifacts", "-ia"}, description = "Comma separated list of groupId:artifactId combination to be ignored."/*, converter = StringToListConverter.class*/)
    private List<String> ignoreArtifacts;

    @Parameter(names = {"--ignoreGroups", "-ig"}, description = "Comma separated list of groupIds (full or part) to be ignored."/*, converter = StringToListConverter.class*/)
    private List<String> ignoreGroups;

    @Parameter(names = {"--deleteAllSnapshots", "-dsn"}, description = "Delete all snapshots irrespective of being latest.")
    private boolean deleteAllSnapshots;

    @Parameter(names = {"--deleteSource", "-dsr"}, description = "Delete sources for all libraries.")
    private boolean deleteSource;

    @Parameter(names = {"--deleteJavadoc", "-djd"}, description = "Delete javadocs for all libraries.")
    private boolean deleteJavadoc;

    @Parameter(names = {"--forceArtifacts", "-fa"}, description = "Comma separated list of groupId:artifactId combination to be deleted."/*, converter = StringToListConverter.class*/)
    private List<String> forceArtifacts;

    @Parameter(names = {"--forceGroups", "-fg"}, description = "Comma separated list of groupIds (full or part) to be deleted."/*, converter = StringToListConverter.class*/)
    private List<String> forceGroups;

    @Parameter(names = {"--dryrun", "-dr"}, description = "Do not delete files, just simulate and print result.")
    private boolean dryRun;

    @Parameter(names = {"--retainOld", "-ro"}, description = "Retain the artifacts even if old versions. Only process the configured inputs.")
    private boolean retainOld;

    static class DateToMillisConverter implements IStringConverter<Long> {

        @Override
        public Long convert(String value) {
            try {
                return DateUtils.parseDate(value, "MM-dd-yyyy").getTime();
            } catch (ParseException e) {
                System.out.println("Could not parse " + value + " as a valid date. Please enter in MM-DD-YYYY format.");
                System.exit(1);
                return 0L;
            }
        }
    }

}