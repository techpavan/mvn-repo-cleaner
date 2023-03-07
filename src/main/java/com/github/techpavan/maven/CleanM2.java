/*
 * Copyright (c) 2018.
 * This code is released under The 3-Clause BSD License.
 * https://github.com/techpavan
 */

package com.github.techpavan.maven;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.concat;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.SystemUtils.USER_HOME;

@Slf4j
public class CleanM2 {
    private static Map<DeleteReason, Set<File>> DELETE_MAP = new LinkedHashMap<>();
    private static Map<String, Set<FileInfo>> PROCESS_MAP = new HashMap<>();
    private static Map<SkipReason, Set<String>> SKIP_MAP = new HashMap<>();
    private static List<File> DELETE_FAILURE_LIST = new ArrayList<>();
    private static ArgData argData = new ArgData();
    private static List<String> RESTRICTED_FILES = Lists.newArrayList("repository.xml", "_maven.repositories", "_remote.repositories", "m2e-lastUpdated.properties", "resolver-status.properties");
    private static List<String> RESTRICTED_PATTERN = Lists.newArrayList("maven-metadata-", ".jar.lastUpdated", ".pom.lastUpdated");
    private static String LS = System.lineSeparator();

    static {
        DELETE_MAP.put(DeleteReason.FORCED_SNAPSHOT, new HashSet<>());
        DELETE_MAP.put(DeleteReason.FORCED_SOURCE, new HashSet<>());
        DELETE_MAP.put(DeleteReason.FORCED_JAVADOC, new HashSet<>());
        DELETE_MAP.put(DeleteReason.ACCESS_DATE, new HashSet<>());
        DELETE_MAP.put(DeleteReason.DOWNLOAD_DATE, new HashSet<>());
        DELETE_MAP.put(DeleteReason.NON_LATEST, new HashSet<>());
        DELETE_MAP.put(DeleteReason.FORCED_ARTIFACT, new HashSet<>());
        DELETE_MAP.put(DeleteReason.FORCED_GROUP, new HashSet<>());

        SKIP_MAP.put(SkipReason.RESERVED, new HashSet<>());
        SKIP_MAP.put(SkipReason.IGNORED_ARTIFACT, new HashSet<>());
        SKIP_MAP.put(SkipReason.IGNORED_GROUP, new HashSet<>());
        SKIP_MAP.put(SkipReason.RETAIN_OLD, new HashSet<>());
        SKIP_MAP.put(SkipReason.LATEST, new HashSet<>());
    }

    public static void main(String[] args) {
        JCommander jCommander = parseInputParams(args);
        File repoDir = evaluateM2Path(jCommander);
        String[] filter = argData.isDeleteSource() || argData.isDeleteJavadoc() ? null : new String[]{"pom"};
        FileUtils.listFiles(repoDir, filter, true).forEach(file -> parseAndEvaluate(file));
        processVersion();
        log.info("*********** Files to be deleted ***********");
        DELETE_MAP.forEach((k, v) -> log.info(LS + LS + k + " : " + LS + StringUtils.join(v.stream().sorted().iterator(), LS)));
        log.info(LS + LS + "*********** Files skipped ***********");
        SKIP_MAP.forEach((k, v) -> log.info(LS + LS + k + " : " + LS + StringUtils.join(v.stream().sorted().iterator(), LS)));
        if (!argData.isDryRun()) {
            log.info(LS + LS + "*********** Beginning to Delete Files ***********");
            deleteMarked();
            log.info(LS + LS + "*********** Deletion Completed ***********");
            log.info(LS + LS + "*********** Files having error in deletion ***********");
            log.info(StringUtils.join(DELETE_FAILURE_LIST.stream().sorted().iterator(), LS));
        } else {
            log.info(LS + LS + "*********** No files were deleted as program was run in DRY-RUN mode  ***********");
        }
    }

    private static File evaluateM2Path(JCommander jCommander) {
        String m2Path = defaultString(argData.getM2Path(), concat(concat(USER_HOME, ".m2"), "repository"));
        File m2Dir = new File(m2Path);
        if (!m2Dir.exists()) {
            log.error("Valid Maven repository could not be found. Please provide a valid input.");
            jCommander.usage();
            System.exit(1);
        }
        return m2Dir;
    }

    private static JCommander parseInputParams(String[] args) {
        JCommander jCommander = JCommander.newBuilder().addObject(argData).build();
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            jCommander.usage();
            System.exit(1);
        }
        return jCommander;
    }

    private static void deleteMarked() {
        DELETE_MAP.values().forEach(fileSet -> {
            fileSet.forEach(file -> {
                if (!FileUtils.deleteQuietly(file)) {
                    DELETE_FAILURE_LIST.add(file);
                }
            });
        });
    }

    private static void processVersion() {
        PROCESS_MAP.forEach((gaId, versionSet) -> {
            String latest = getLatestVersion(versionSet);
            versionSet.forEach(fileInfo -> {
                if (!latest.equals(fileInfo.getVersion())) {
                    DELETE_MAP.get(DeleteReason.NON_LATEST).add(fileInfo.getFile());
                } else {
                    SKIP_MAP.get(SkipReason.LATEST).add(fileInfo.getFile().getAbsolutePath());
                }
            });
        });
    }

    private static String getLatestVersion(Set<FileInfo> versionSet) {
        List<String> versionList = versionSet.stream().map(f -> f.getVersion()).collect(Collectors.toList());
        String latest = versionList.stream().map(v -> new ComparableVersion(v)).sorted().reduce((a, b) -> b).get().toString();
        return latest;
    }

    private static void parseAndEvaluate(File file) {
        try {
            if (isRestrictedFile(file.getName())) {
                SKIP_MAP.get(SkipReason.RESERVED).add(file.getAbsolutePath());
                return;
            }
            FileInfo fileInfo = createFileInfo(file);

            if (checkIgnoredFile(fileInfo)) {
                return;
            }
            DeleteReason deleteReason = checkForcedDeleteReason(fileInfo);
            if (deleteReason != null) {
                if (DeleteReason.FORCED_ARTIFACT == deleteReason) {
                    DELETE_MAP.get(deleteReason).add(fileInfo.getParentFileInfo().getParentFileInfo().getFile());
                } else if (DeleteReason.FORCED_GROUP == deleteReason) {
                    DELETE_MAP.get(deleteReason).add(fileInfo.getParentFileInfo().getParentFileInfo().getParentFileInfo().getFile());
                } else {
                    DELETE_MAP.get(deleteReason).add(file);
                }
                return;
            }

            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            // For below cases, add entire folder instead of just individual files
            if (attributes.lastAccessTime().toMillis() < argData.getAccessedBefore()) {
                DELETE_MAP.get(DeleteReason.ACCESS_DATE).add(file.getParentFile());
                return;
            }
            if (attributes.lastAccessTime().toMillis() > argData.getAccessedAfter()) {
                DELETE_MAP.get(DeleteReason.ACCESS_DATE).add(file.getParentFile());
                return;
            }

            if (attributes.lastModifiedTime().toMillis() < argData.getDownloadedBefore()) {
                DELETE_MAP.get(DeleteReason.DOWNLOAD_DATE).add(file.getParentFile());
                return;
            }
            if (attributes.lastModifiedTime().toMillis() > argData.getDownloadedAfter()) {
                DELETE_MAP.get(DeleteReason.DOWNLOAD_DATE).add(file.getParentFile());
                return;
            }

            if (argData.isRetainOld()) {
                SKIP_MAP.get(SkipReason.RETAIN_OLD).add(file.getParentFile().getAbsolutePath());
                return;
            }
            // When none of the above are matched, add it for latest / oldest processing
            addToProcessMap(fileInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isRestrictedFile(String name) {
        if (RESTRICTED_FILES.contains(name)) {
            return true;
        }
        return RESTRICTED_PATTERN.stream().filter(pattern -> name.contains(pattern)).count() > 0;
    }

    private static void addToProcessMap(FileInfo fileInfo) {
        String gaId = fileInfo.getGroupId() + ":" + fileInfo.getArtifactId();
        if (PROCESS_MAP.get(gaId) == null) {
            PROCESS_MAP.put(gaId, new HashSet<>());
        }
        PROCESS_MAP.get(gaId).add(fileInfo.getParentFileInfo());
    }

    private static DeleteReason checkForcedDeleteReason(FileInfo fileInfo) {
        if (argData.isDeleteJavadoc() && fileInfo.getFile().getName().contains(fileInfo.getVersion() + "-javadoc.jar")) {
            return DeleteReason.FORCED_JAVADOC;
        } else if (argData.isDeleteSource() && fileInfo.getFile().getName().contains(fileInfo.getVersion() + "-sources.jar")) {
            return DeleteReason.FORCED_SOURCE;
        } else if (argData.isDeleteAllSnapshots() && fileInfo.getVersion().endsWith("-SNAPSHOT")) {
            return DeleteReason.FORCED_SNAPSHOT;
        } else if (argData.getForceArtifacts() != null && argData.getForceArtifacts().contains(fileInfo.getGroupId() + ":" + fileInfo.getArtifactId())) {
            return DeleteReason.FORCED_ARTIFACT;
        } else if (argData.getForceGroups() != null && argData.getForceGroups().contains(fileInfo.getGroupId())) {
            return DeleteReason.FORCED_GROUP;
        }
        return null;
    }

    private static FileInfo createFileInfo(File file) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFile(file);
        fileInfo.setArtifactId(findArtifactId(file));
        fileInfo.setGroupId(findGroupId(file, fileInfo.getArtifactId()));
        fileInfo.setVersion(findVersion(file));
        return fileInfo;
    }

    private static boolean checkIgnoredFile(FileInfo fileInfo) {
        if (argData.getIgnoreArtifacts() != null && argData.getIgnoreArtifacts().contains(fileInfo.getGroupId() + ":" + fileInfo.getArtifactId())) {
            SKIP_MAP.get(SkipReason.IGNORED_ARTIFACT).add(fileInfo.getParentFileInfo().getParentFileInfo().getFile().getAbsolutePath());
            return true;
        } else if (argData.getIgnoreGroups() != null && argData.getIgnoreGroups().contains(fileInfo.getGroupId())) {
            SKIP_MAP.get(SkipReason.IGNORED_GROUP).add(fileInfo.getParentFileInfo().getParentFileInfo().getParentFileInfo().getFile().getAbsolutePath());
            return true;
        }
        return false;
    }

    private static String findGroupId(File file, String artifactId) {
        List<String> split = Arrays.asList(StringUtils.split(file.getAbsolutePath(), File.separatorChar));
        return StringUtils.join(split.stream().skip(split.indexOf("repository") + 1).limit(split.lastIndexOf(artifactId) - split.indexOf("repository") - 1).collect(Collectors.toList()), ".");
    }

    private static String findArtifactId(File file) {
        return file.getParentFile().getParentFile().getName();
    }

    private static String findVersion(File file) {
        return file.getParentFile().getName();
    }

    private enum DeleteReason {
        ACCESS_DATE,
        DOWNLOAD_DATE,
        FORCED_SNAPSHOT,
        FORCED_SOURCE,
        FORCED_JAVADOC,
        FORCED_GROUP,
        FORCED_ARTIFACT,
        NON_LATEST
    }

    private enum SkipReason {
        IGNORED_ARTIFACT, IGNORED_GROUP, LATEST, RESERVED, RETAIN_OLD
    }

}
