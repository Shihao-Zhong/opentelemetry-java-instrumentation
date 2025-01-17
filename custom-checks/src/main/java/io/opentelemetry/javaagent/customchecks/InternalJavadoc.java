/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.customchecks;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.tools.javac.api.JavacTrees;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

@AutoService(BugChecker.class)
@BugPattern(
    name = "OtelInternalJavadoc",
    summary =
        "This public internal class doesn't end with the javadoc disclaimer: \""
            + InternalJavadoc.EXPECTED_INTERNAL_COMMENT
            + "\"",
    severity = WARNING)
public class InternalJavadoc extends BugChecker implements BugChecker.ClassTreeMatcher {

  private static final long serialVersionUID = 1L;

  private static final Pattern INTERNAL_PACKAGE_PATTERN = Pattern.compile("\\binternal\\b");

  private static final Pattern EXCLUDE_PACKAGE_PATTERN =
      Pattern.compile("^io\\.opentelemetry\\.javaagent\\.instrumentation\\.internal\\.");

  static final String EXPECTED_INTERNAL_COMMENT =
      "This class is internal and is hence not for public use."
          + " Its APIs are unstable and can change at any time.";

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!isPublic(tree) || !isInternal(state)) {
      return Description.NO_MATCH;
    }
    String javadoc = getJavadoc(state);
    if (javadoc != null && javadoc.endsWith(EXPECTED_INTERNAL_COMMENT)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static boolean isPublic(ClassTree tree) {
    return tree.getModifiers().getFlags().contains(Modifier.PUBLIC);
  }

  private static boolean isInternal(VisitorState state) {
    PackageTree packageTree = state.getPath().getCompilationUnit().getPackage();
    if (packageTree == null) {
      return false;
    }
    String packageName = state.getSourceForNode(packageTree.getPackageName());
    return packageName != null
        && INTERNAL_PACKAGE_PATTERN.matcher(packageName).find()
        && !EXCLUDE_PACKAGE_PATTERN.matcher(packageName).find();
  }

  @Nullable
  private static String getJavadoc(VisitorState state) {
    DocCommentTree docCommentTree =
        JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree == null) {
      return null;
    }
    return docCommentTree.toString().replace("\n", "");
  }
}
