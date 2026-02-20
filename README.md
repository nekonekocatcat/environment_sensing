# 環境コレクション

小型軽量な環境センサを持ち歩いて自分の生活する環境をコレクションするゲームです
取得した環境データをノーマル環境・レア環境の2種類に分類しています

| No. | ノーマル環境 | レア環境 |
|---:|---|---|
| 1 | 静かめ快適環境 | 熱帯低気圧レア環境 |
| 2 | リビングまったり環境 | クラブわいわいレア環境 |
| 3 | 夜ふかしの薄暗い部屋 | 工事現場みたいなレア環境 |
| 4 | 早朝の静けさ環境 | 真夏の密室レア環境 |
| 5 | 空気こもり気味環境 | 南国リゾートレア環境 |
| 6 | 作業はかどり環境 | 星空キャンプレア環境 |
| 7 | 集中できないザワザワ環境 | 勉強はかどる集中レア環境 |
| 8 | 明るい屋外っぽい環境 | カラオケ大会レア環境 |
| 9 | 交通量多め環境 | 焚き火レア環境 |
|10 | カフェっぽい環境 | 電車ラッシュレア環境 |
|11 | フードコートっぽい環境 | 映画館レア環境 |
|12 | 調理中っぽい環境 | ととのいサウナっぽいレア環境 |
|13 | カラカラ環境 | めっちゃ静かレア環境 |
|14 | じめじめ環境 | まるで北極レア環境 |
|15 | 冷房つよめ環境 | 無響室レア環境 |
|16 | 暗い静か環境 | オーロラレア環境 |
|17 | ざわざわ環境 | 真空スーパーレア環境 |
|18 | 涼しめ明るい環境 | ブラックホール直前環境 |
|19 |  | 火星コロニーレア環境 |

## 使用する環境センサ
OMRON USB型環境センサを使用
もう販売停止になってしまっています
https://www.fa.omron.co.jp/products/family/3724/download/catalog.html

<img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/0144ae3c-9312-41da-95af-77bc973c9617" />

## 起動方法

1. 前提
- AndroidStudio
- JDK17
- 実機
がある前提でお願いします
（エミュレータ使用時はメモリ不足でビルドできない場合があるので実機推奨）

2. リポジトリをクローン
- ターミナル上で
```bash
git clone https://github.com/nekonekocatcat/environment_sensing.git
cd environment_sensing
```
を実行してください

3. AndroidStudioで開く
- AndroidStudio起動後，File > Openを選択する
- クローンした environment_sensing ディレクトリを指定

Gradle の同期に時間がかかるのでここで休憩してください

4. Google Maps API キーを設定
- マップ機能にGoogle MapsのAPIを使用しているため，APIキーを指定します
- `gradle.properties` に以下を追加してください。
```bash
MAPS_API_KEY=YOUR_API_KEY_HERE
```
※ `MAPS_API_KEY` は GitHub には含まれていません  
※ 各自で Google Cloud Console から取得してください

5. Gradle を再同期
- `gradle.properties` を編集した後、AndroidStudio 上部の **Sync Now** を押す
- エラーが出なければ次へ進む

6. 実機を接続
- USB で Android 端末を PC に接続
- 端末側でUSBデバッグを有効化
- AndroidStudio のデバイス選択欄に実機が表示されるのを確認

7. アプリをビルド・起動
- AndroidStudio 上部の ▶（Run）を押す
- 初回はビルドに時間がかかるので休憩してください

8. 初期設定
- アプリ起動後、必要な権限を許可
  - 位置情報
  - Bluetooth
  - 通知（Android 13 以降）
- 「コレクション開始」ボタンを押すと計測開始


## アプリ構成

- `LogService`
  - バックグラウンドで BLE スキャンとセンサデータ取得を行うフォアグラウンドサービス
- `SensorLogger`
  - センサ値の蓄積・環境判定・DB保存を担当
- `RareEnvironmentChecker / NormalEnvironmentChecker`
  - センサ値から環境判定を行うロジック
- `CollectionScreen`
  - 獲得済み環境の一覧表示（図鑑画面）
- `HistoryScreen`
  - 環境獲得履歴の時系列表示

## データフロー概要

1. 環境センサが BLE Advertisement により 1 秒間隔でデータを送信
2. アプリのフォアグラウンドサービスがバックグラウンドで BLE スキャンを実行
3. 受信したバイト列をパースして温度・湿度・気圧・騒音・照度・TVOC・CO₂を取得
4. センサ値を元にノーマル環境・レア環境を判定
5. 判定結果を Room Database に保存
6. コレクション画面・履歴画面に即時反映
