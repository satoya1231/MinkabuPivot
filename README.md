# Nova Launcher

シンプルなAndroidホームランチャーのMVPです。

## 主な機能
- HOMEアプリとして登録
- インストール済みアプリ一覧
- アプリ検索
- アプリ起動
- お気に入り固定
- 登録済みアプリを横一列のバーで選択・起動
- 登録済みアプリのフォルダ作成・編集・削除
- フォルダ編集画面でアプリを検索
- ホーム画面のメニューから各種操作
- ホーム画面にお気に入りを4列表示
- 既定のホームアプリ選択画面を開く
- ダークテーマ
- アプリ一覧の再読込

## 開発環境
- Android Studio Quail 4 (2026.1.4) 以降推奨
- JDK 17
- Android SDK 37
- Android Gradle Plugin 9.4.0
- Jetpack Compose BOM 2026.08.00
- minSdk 26 / targetSdk 37

## GitHub ActionsでAPKを自動生成
GitHubへpushすると `.github/workflows/build-apk.yml` がAPKを自動ビルドします。

### AndroidスマホからAPKを取得
1. GitHubでこのリポジトリを開く
2. `Actions` → 最新の `Build Android APK`
3. `Artifacts` → `NovaLauncher-debug-apk` をダウンロード
4. ZIPを展開し `app-debug.apk` をタップしてインストール
5. Nova Launcherを既定のホームアプリに設定
