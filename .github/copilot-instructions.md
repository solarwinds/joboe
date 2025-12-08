# GitHub Copilot Instructions

## Priority Guidelines

When generating code for this repository:

1. **Version Compatibility**: Always detect and respect the exact versions of languages, frameworks, and libraries used in this project
2. **Context Files**: Prioritize patterns and standards defined in the .github/copilot directory (when available)
3. **Codebase Patterns**: When context files don't provide specific guidance, scan the codebase for established patterns
4. **Architectural Consistency**: Maintain our modular architecture with clear separation of concerns across modules
5. **Code Quality**: Prioritize maintainability, testability, and consistency in all generated code


## Project Architecture

### Module Structure
This is a multi-module Maven project with the following modules:
- **core**: Main agent functionality, event reporting, RPC communication, BSON encoding
- **config**: Configuration management, environment variable handling, JSON config reading
- **logging**: Custom logging framework with file rotation and level control
- **metrics**: Metrics collection, JMX monitoring, custom metrics, framework detection
- **sampling**: Sampling decisions, trace context, settings management

### Package Organization
- Follow the `com.solarwinds.joboe.<module>` package structure
- Keep related functionality in the same package
- Use sub-packages for distinct functional areas

## Code Quality Standards

### Maintainability

**Naming Conventions:**
- Follow standard Java naming conventions

**Code Organization:**
- Follow `SOLID` principles
- Methods should be small enough to fit within the visible area of a standard IDE window without scrolling.

### Testability
- Match dependency injection approaches used in the codebase
- Apply the same patterns for managing dependencies
- Follow established mocking and test double patterns
- Match the testing style used in existing tests


### Code Comments
- Use comments sparingly and reserve them for obtuse code

## Build and Development

### Maven Commands
- `mvn clean install`: Build all modules
- `mvn verify`: Run all tests
- `mvn -s .github/m2/settings.xml deploy`: Deploy to GitHub Packages
- Always build from root to ensure proper module dependency resolution

## File-Type Specific Instructions

This repository includes detailed instructions for different file types in `.github/copilot/instructions/`:

- **[java.md](copilot/instructions/java.md)**: Complete Java 8 coding standards, patterns, and best practices

## When in Doubt

- Follow best practices
- Look for similar existing implementations in the codebase
- Remember: this is a Java 8 library that must remain backward compatible
