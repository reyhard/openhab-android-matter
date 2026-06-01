#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs";

const args = parseArgs(process.argv.slice(2));
const tag = args.tag ?? args.version ?? "";
const toRef = args.to ?? (tag && refExists(tag) ? tag : "HEAD");
const repo = args.repo ?? process.env.GITHUB_REPOSITORY ?? "";
const output = args.output ?? "";
const fromRef = args.from ?? findPreviousTag(toRef);

const commits = readCommits(fromRef, toRef).filter((commit) => !shouldSkip(commit));
const sections = [
  ["breaking", "Breaking Changes"],
  ["matter", "Matter Commissioning"],
  ["features", "Features"],
  ["fixes", "Fixes"],
  ["build", "Build, CI, and Release"],
  ["docs", "Documentation"],
  ["tests", "Tests"],
  ["other", "Other Changes"],
];
const grouped = Object.fromEntries(sections.map(([key]) => [key, []]));

for (const commit of commits) {
  grouped[classify(commit)].push(formatCommit(commit, repo));
}

const lines = [];
if (tag) {
  lines.push(`# ${tag}`, "");
}
lines.push("## What's Changed", "");

let wroteSection = false;
for (const [key, title] of sections) {
  if (grouped[key].length === 0) {
    continue;
  }
  wroteSection = true;
  lines.push(`### ${title}`);
  lines.push(...grouped[key].map((entry) => `- ${entry}`));
  lines.push("");
}

if (!wroteSection) {
  lines.push("- No user-facing changes were detected from commit subjects.", "");
}

if (repo && fromRef && tag) {
  lines.push(`**Full Changelog**: https://github.com/${repo}/compare/${fromRef}...${tag}`);
} else if (repo && tag) {
  lines.push(`**Full Changelog**: https://github.com/${repo}/commits/${tag}`);
}

const changelog = `${lines.join("\n").trim()}\n`;
if (output) {
  fs.writeFileSync(output, changelog, "utf8");
} else {
  process.stdout.write(changelog);
}

function parseArgs(values) {
  const parsed = {};
  for (let i = 0; i < values.length; i += 1) {
    const value = values[i];
    if (!value.startsWith("--")) {
      throw new Error(`Unexpected argument: ${value}`);
    }
    const name = value.slice(2);
    const next = values[i + 1];
    if (!next || next.startsWith("--")) {
      parsed[name] = "true";
      continue;
    }
    parsed[name] = next;
    i += 1;
  }
  return parsed;
}

function findPreviousTag(ref) {
  try {
    return execGit(["describe", "--tags", "--abbrev=0", "--match", "v*", `${ref}^`]).trim();
  } catch {
    try {
      return execGit(["describe", "--tags", "--abbrev=0", `${ref}^`]).trim();
    } catch {
      return "";
    }
  }
}

function readCommits(from, to) {
  const range = from ? `${from}..${to}` : to;
  const raw = execGit(["log", "--format=%H%x1f%an%x1f%s%x1f%b%x1e", range]);
  return raw
    .split("\x1e")
    .map((record) => record.trim())
    .filter(Boolean)
    .map((record) => {
      const [hash, author, subject, ...bodyParts] = record.split("\x1f");
      return {
        hash,
        author,
        subject: subject.trim(),
        body: bodyParts.join("\x1f").trim(),
      };
    })
    .reverse();
}

function shouldSkip(commit) {
  const text = `${commit.subject}\n${commit.body}`.toLowerCase();
  return text.includes("[skip changelog]") || text.includes("changelog: skip");
}

function classify(commit) {
  const parsed = parseConventionalSubject(commit.subject);
  const subject = commit.subject.toLowerCase();
  const scope = (parsed.scope ?? "").toLowerCase();
  const body = commit.body.toLowerCase();

  if (parsed.breaking || body.includes("breaking change:")) {
    return "breaking";
  }
  if (
    ["matter", "chip", "connectedhomeip", "commissioning", "thread", "ocw", "ble"].includes(scope) ||
    /\b(matter|chip|connectedhomeip|commissioning|thread|ocw|ble|attestation|fabric)\b/.test(subject)
  ) {
    return "matter";
  }
  if (parsed.type === "feat") {
    return "features";
  }
  if (parsed.type === "fix") {
    return "fixes";
  }
  if (["ci", "build", "release"].includes(parsed.type)) {
    return "build";
  }
  if (parsed.type === "docs") {
    return "docs";
  }
  if (["test", "tests"].includes(parsed.type)) {
    return "tests";
  }
  return "other";
}

function formatCommit(commit, repo) {
  const parsed = parseConventionalSubject(commit.subject);
  let summary = parsed.description || commit.subject;
  summary = stripTrailingPeriod(capitalize(summary.trim()));

  const pr = commit.subject.match(/\(#(\d+)\)$/);
  const hash = commit.hash.slice(0, 7);
  const commitRef = repo ? `[${hash}](https://github.com/${repo}/commit/${commit.hash})` : hash;
  const prRef = repo && pr ? ` [#${pr[1]}](https://github.com/${repo}/pull/${pr[1]})` : "";

  return `${summary} by ${commit.author} in ${commitRef}${prRef}`;
}

function parseConventionalSubject(subject) {
  const match = subject.match(/^([a-zA-Z][a-zA-Z0-9-]*)(?:\(([^)]+)\))?(!)?:\s+(.+)$/);
  if (!match) {
    return { type: "", scope: "", breaking: false, description: subject };
  }
  return {
    type: match[1].toLowerCase(),
    scope: match[2] ?? "",
    breaking: match[3] === "!",
    description: match[4],
  };
}

function capitalize(value) {
  return value.length > 0 ? `${value[0].toUpperCase()}${value.slice(1)}` : value;
}

function stripTrailingPeriod(value) {
  return value.endsWith(".") ? value.slice(0, -1) : value;
}

function execGit(args) {
  return execFileSync("git", args, { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });
}

function refExists(ref) {
  try {
    execGit(["rev-parse", "--verify", `${ref}^{commit}`]);
    return true;
  } catch {
    return false;
  }
}
