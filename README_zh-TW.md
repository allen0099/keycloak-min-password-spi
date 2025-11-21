# Keycloak 最小密碼使用期限 SPI

這是一個自訂的 Keycloak SPI (Service Provider Interface)，實作了「最小密碼使用期限」的密碼政策。它能防止使用者在設定密碼後的特定時間內再次更改密碼。

## 功能

-   **最小期限限制**：如果目前密碼設定的時間太短，將阻止使用者更改密碼。
-   **可設定單位**：支援秒 (Seconds)、分 (Minutes)、時 (Hours) 和天 (Days)。
-   **管理員豁免**：管理員透過 Admin Console 重設使用者密碼時，**不受**此政策限制。
-   **臨時密碼豁免**：被強制要求更新密碼的使用者（例如：首次登入使用臨時密碼），**不受**此政策限制。
-   **穩健的解析**：處理空白字元和大小寫不敏感。無效的設定將被安全地忽略（政策失效）並記錄警告日誌。

## 安裝

### 選項 1：下載 Release（推薦）

1.  從 [Releases 頁面](https://github.com/allen0099/keycloak-min-password-spi/releases) 下載最新的 JAR 檔案。

2.  **部署到 Keycloak**：
    -   將 JAR 檔案複製到 Keycloak 安裝目錄下的 `providers/` 資料夾。
    -   執行 Keycloak 建置指令：
        ```bash
        bin/kc.sh build
        ```

3.  **重新啟動 Keycloak**。

### 選項 2：從原始碼建置

1.  **建置 JAR 檔**：
    ```bash
    mvn clean package
    ```
    這將會產生 `target/keycloak-min-password-age-spi-1.0.0-SNAPSHOT.jar`。

2.  請依照上述「部署到 Keycloak」步驟進行。

## 設定

1.  登入 Keycloak Admin Console。
2.  前往 **Authentication** -> **Password Policy**。
3.  點擊 **Add Policy**（或在較新版本中從列表中選擇）。
4.  選擇 **Minimum Password Age (Seconds/Time)**。
5.  輸入時間限制。您可以使用以下格式：
    -   **秒數**：僅輸入數字（例如：`60` 代表 60 秒）。
    -   **時間單位**：數字後接單位（例如：`1:d` 代表 1 天，`30:m` 代表 30 分鐘，`12:h` 代表 12 小時）。
6.  點擊 **Save**。

### 驗證說明
-   **無效格式**：如果您輸入無效的格式（例如：`abc`、`10:years`），為了安全起見，該政策將會**失效**，並且會在 Keycloak 伺服器日誌中記錄一條警告。
-   **負數**：負數將被視為 `0`（失效）。
-   **UI 驗證**：請注意，Keycloak Admin UI 可能無法阻止您儲存無效的文字。如果政策似乎沒有生效，請檢查伺服器日誌。

## 使用方式

-   **使用者**：如果使用者試圖在期限到期前更改密碼，他們將會看到一條錯誤訊息，指出他們必須等待多久。
-   **管理員**：無論此政策如何設定，管理員都可以隨時重設使用者密碼。

## 發布流程

本專案使用 GitHub Actions 自動發布 artifact。

1.  更新 `pom.xml` 中的版本號（例如 `1.0.0`）。
2.  提交更改。
3.  建立一個以 `v` 開頭的標籤（例如 `v1.0.0`）。
4.  將標籤推送到 GitHub。

Workflow 將會自動建置 JAR 檔並建立一個附帶 artifact 的 GitHub Release。
