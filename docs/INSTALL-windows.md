# Installation Guide for Windows

## Prerequisites
1. Download the required files from the [windows folder](../windows) in the project repository:
   - `yafvajar.exe`
   - `yafvajar.xml`
   - `application.yaml`
2. Download the `yafva.jar` file from the [latest release](https://github.com/Outburn-IL/yafva.jar/releases).
3. Ensure you have all four files: `yafvajar.exe`, `yafvajar.xml`, `application.yaml`, and `yafva.jar`.

> **Note:** This guide uses WinSW version `2.12.0.0`.
> **Configuration:** For detailed information about configuration options in `application.yaml`, refer to the [Configuration Reference](./CONFIGURATION.md).

## Installation Steps
1. Place the `yafvajar.exe`, `yafvajar.xml`, `application.yaml`, and `yafva.jar` files
   in the same directory (e.g., `C:\yafva-validator`).
2. Open a command prompt with administrative privileges.
3. Navigate to the directory where the files are located.
4. Run the following command to install the service:
   ```
   yafvajar.exe install
   ```
5. Start the service with:
   ```
   yafvajar.exe start
   ```

## Notes
- To uninstall the service, use:
  ```
  yafvajar.exe uninstall
  ```

- For additional configuration options, refer to the [WinSW documentation](https://github.com/winsw/winsw).
