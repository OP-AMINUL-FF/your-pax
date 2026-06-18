# 🤝 Contributing to your-pax

We welcome contributions to your-pax! To make sure the process goes smoothly, please follow these guidelines:

## 📋 Code of Conduct

Please note that all participants in our project are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md). Make sure to review it before contributing.

## 🛠 How to Contribute

1. **Fork the repository**:
   Fork the project to your GitHub account using the GitHub interface.

2. **Create a new branch**:
   Use a descriptive branch name for your feature or bugfix:

   git checkout -b feature/your-feature-name

3. **Make your changes**:
   Implement your feature or fix the bug in your branch. Make sure to include tests where applicable and follow coding standards.

4. **Test your changes**:
   Run the project to ensure your changes don't break any functionality:

   ```bash
   python3 your-pax.py
   ```
   Check the web UI at `http://<device-ip>:8000` and verify the relevant feature works.
5. **Commit your changes**:
   Use meaningful commit messages that explain what you have done:

   git commit -m "Add feature/fix: Description of changes"

6. **Push your changes**:
   Push your changes to your fork:

   git push origin feature/your-feature-name

7. **Submit a Pull Request**:
   Create a pull request on the main repository, detailing the changes you’ve made. Link any issues your changes resolve and provide context.

## 📑 Guidelines for Contributions

- **Lint your code** before submitting a pull request. We use [pylint](https://www.pylint.org/) for Python linting (see `.pylintrc`).
- Ensure **test coverage** for your code. Uncovered code may delay the approval process.
- Write clear, concise **commit messages**.

Thank you for helping improve!

---

## 📜 License

2024 - your-pax is distributed under the MIT License. For more details, please refer to the [LICENSE](LICENSE) file included in this repository.
