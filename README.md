# ルーレット (roulette-android)

Android 向けのシンプルなルーレットアプリです。テキストボックスに 1 行 1 項目で入力するだけで、
すぐにルーレットを作成して回せます。

[![Android CI](https://github.com/yuushi2024/roulette-android/actions/workflows/android.yml/badge.svg)](https://github.com/yuushi2024/roulette-android/actions/workflows/android.yml)

## 主な機能

- **かんたん入力** — テキストボックスに 1 行ずつ項目を書くだけでルーレットを作成します。
- **自動保存** — 入力内容は端末内 (SharedPreferences) に保存され、アプリを閉じても残ります。
- **一括クリア** — 「クリア」ボタンで入力をまとめて削除できます (確認ダイアログ付き)。
- **ワクワクする盤面** — 各項目を複数セクターに分割して盤面を細かくし、同じ項目が隣り合わない
  ように分散配置します。止まるまで結果が分かりにくくなります。
- **鮮やかな配色** — 項目ごとに色相を黄金角で大きく離し、同じ項目のセクターは明度・彩度を
  変えながらグラデーションにします。項目が 2〜3 個でも単調になりません。文字色は
  セクターの明るさに応じて黒／白を切り替えるので、明るい色でも読みやすいままです。
- **改行で複数項目** — 入力欄ではソフトキーボードの Enter がそのまま改行になり、
  1 行 1 項目を入力できます。

## ダウンロード

署名済み APK は [`release/`](release/) にあります。

| ファイル | バージョン | 備考 |
| --- | --- | --- |
| `roulette-android-1.0.0.apk` | 1.0.0 | リリース署名済み |

インストール前にチェックサムを確認できます。

```bash
sha256sum -c release/SHA256SUMS.txt
```

## インストール

1. `release/roulette-android-1.0.0.apk` を端末にコピーします。
2. 端末の設定で「提供元不明のアプリ」のインストールを許可します。
3. ファイルマネージャーから APK を開いてインストールします。

対応バージョン: Android 5.0 (API 21) 以降。

## 使い方

1. 画面下のテキストボックスに項目を 1 行ずつ入力します。
2. 「スタート」を押すとルーレットが回転します。
3. ポインタが止まった位置の項目が「結果」に表示されます。
4. 「クリア」で入力をすべて削除できます。

## ビルド

必要なもの: JDK 17 以降、Android SDK (platform 34 / build-tools 34.0.0)。

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:testDebugUnitTest   # 単体テスト
./scripts/build-release.sh         # 署名済み APK を release/ に出力
```

`scripts/build-release.sh` は、署名鍵がなければ初回に鍵を自動生成し、パスワードは
環境変数の指定を優先、未指定ならランダムに生成して `keystore/keystore.properties` に
書き出します。鍵 (`keystore/`) は
`.gitignore` 済みで、リポジトリには含まれません。**パスワードを失うと鍵を使い回せない**
ため、`keystore.properties` は安全な場所に保管してください。

パスワードは環境変数 `ROULETTE_STORE_PASSWORD` / `ROULETTE_KEY_PASSWORD`、鍵の場所と
別名は `ROULETTE_KEYSTORE` / `ROULETTE_KEY_ALIAS` でも指定できます。パスワード未設定で
鍵だけ存在する場合、Gradle は**署名なし**の APK を生成します (固定パスワードへの
フォールバックはしません)。

## 構成

```
app/src/main/java/com/yuushi2024/roulette/
├── MainActivity.kt          # 画面と入力の制御
├── data/PreferencesStore.kt # 入力内容の永続化
├── logic/WheelFactory.kt    # 入力 → セクター分割・分散配置
├── logic/SpinMath.kt        # 回転角 → 当選セクターの判定
└── ui/RouletteView.kt       # 盤面の描画と回転アニメーション
```

`logic/` は Android に依存しない純粋な Kotlin なので、単体テストで検証しています。

## ライセンス

[MIT](LICENSE)
