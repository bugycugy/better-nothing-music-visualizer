# <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Fire.png" alt="Fire" width="35" height="35" /> Better Nothing Müzik Görselleştirici

<img 
  src="https://img.shields.io/github/downloads/Aleks-Levet/better-nothing-music-visualizer/total?style=for-the-badge&logo=github&label=Total%20app%20downloads%20from%20github:&color=ff0000&labelColor=000000"
  style="height:40px; border-radius:12px;"> 
  
Diğer dillerde oku: 🇺🇸 [English](README.md) | 🇮🇳 [हिन्दी](README_HI.md)

## <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Partying%20Face.png" alt="Partying Face" width="25" height="25" /> Android Uygulaması Geldi!
Basit Python betiğinden güçlü bir Android uygulamasına başarıyla geçiş yaptık! Cihazınızdaki canlı ses akışını Media Projection (Medya Yansıtma) kullanarak yakalıyor ve doğrudan gliflere işliyor. Bu, Spotify, YouTube Music ve temel olarak diğer tüm uygulamalardan gelen müziği manuel işlemeye gerek kalmadan görselleştirebileceğiniz anlamına geliyor! Artık sadece yerel dosyalarla sınırlı değilsiniz!
## <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Partying%20Face.png" alt="Partying Face" width="25" height="25" /> The Android App is here!
We have successfully moved from the simple Python script to a powerful Android app! It grabs the live audio stream from your device using **Media Projection** and processes it directly into the glyphs. This means you can visualize music from **Spotify, YouTube Music**, and basically any other app without manual processing! No more local files only!

## <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Thinking%20Face.png" alt="Thinking Face" width="25" height="25" /> Bu neden var?
Pek çok insan için (ben dahil), Nothing tarafından sağlanan stok Glyph Müzik Görselleştirmesi rastgele hissettiriyor. Teknik olarak öyle olmasa bile, müziğe verilen görsel tepki pek belirgin değil. Üstelik bu özellik, Glyph Arayüzü'nün tüm potansiyelini gerçekten kullanmıyor. İşte bu yüzden kendi müzik görselleştiricimi yaptım.

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/2696_fe0f/512.gif" alt="⚖" width="32" height="32"> Stok ve Better Müzik Görselleştirici karşılaştırma tablosu:
| Özellik | Nothing Stok | **Better Müzik Görselleştirici** |
| :--- | :--- | :--- |
| **Işık seviyeleri** | ~2-bit derinlik (3 ışık seviyesi) | **12-bit derinlik (4096 ışık seviyesi)** |
| **Kare Hızı** | ~25 FPS | **60 FPS** |
| **Hassasiyet** | Rastgele hissettiriyor, senkronizasyonu görmek zor | **Her ışığın yoğunluğunu hassas bir şekilde belirlemek için FFT analizi kullanır** |
| **Bölgeler** | Standart, tüm fiziksel glifler kullanılır | **Her glif segmenti ve alt bölgesi bağımsız olarak kullanılır ve kontrol edilir** |
| **Görselleştirme yöntemi** | Sadece gerçek zamanlı | **20ms'ye kadar gecikme ile gerçek zamanlı veya önceden işlenmiş ses dosyaları** |

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f3ac/512.gif" alt="🎬" width="40" height=""> [Video demos and examples](https://github.com/Aleks-Levet/better-nothing-music-visualizer/blob/main/Demo-video-examples.md)

### Farkı iş başında görün! [**Video demolarımıza kolayca göz atmak için buraya tıklayın!**](https://github.com/Aleks-Levet/better-nothing-music-visualizer/blob/main/Demo-video-examples.md)

## 📲 Desteklenen Nothing Phone Modelleri
Şu anda şu modeller desteklenmektedir:
- Nothing phone (1)
  - Uygulama için glif hata ayıklama (debug) modunun **AÇIK** olması gerekir, bu bir *ADB komutu* ile ayarlanır: `adb shell settings put global nt_glyph_interface_debug_enable 1`. Nothing bize API anahtarını verdiğinde bu düzeltilecek.
- Nothing phone (2)
- Nothing phone (2a)
- Nothing phone (2a plus)
- Nothing phone (3a)
- Nothing phone (3a pro)
- *Nothing phone (3)* **(beta, henüz tam iyi değil)**
- *Nothing Phone (4a)*
- *Nothing Phone (4a pro)*

### <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/2699_fe0f/512.gif" alt="⚙" width="25" height="25"> Nasıl çalışır (teknik olarak)
- Yüksek kaliteli bir ses akışı yakalanır.
- **FFT (Hızlı Fourier Dönüşümü)**, her **16.666 ms'lik** kare (60 FPS) için **20 ms'lik** bir pencerede frekansları analiz etmek amacıyla kullanılır, bu da görselleştirmeyi daha doğru hale getirir.
- Her glif bölgesi için **frekans aralıkları** `zones.config` dosyasında tanımlanmıştır ve tamamen özelleştirilebilir.
- Her bir glifin **parlaklığı**, kendisine atanan frekans aralığında bulunan **tepe büyüklüğü (peak magnitude)** ile tanımlanır. Bu, farklı frekans "bölgelerinin" ne kadar yüksek olduğunu ölçer.
- Animasyonu daha pürüzsüz hale getirirken duyarlılığı korumak için yalnızca **aşağı yönlü yumuşatma** uygulanır (bu işin sırrı).
- Ve sonrasında gliflerde gösterilmeye hazır hale gelir!

## 📖 Uygulama nasıl kullanılır?
1. **En son APK'yı indirin**: Yayınlar (releases) bölümünden en güncel sürümü edinin.
2. **İzinleri verin**: Uygulama, Ekran Yakalama (Medya Yansıtma) ve Bildirim erişimi gerektirir.
3. **Görselleştirmeye başlayın**: "Start" (Başlat) düğmesine basın ve herhangi bir uygulamadan müzik çalın!
4. **Gecikmeyi ayarlayın**: Işıklar Bluetooth hoparlörünüz veya kulaklığınızla tam senkronize değilse, gecikme eklemek veya çıkarmak için **Audio** (Ses) sekmesini kullanın.
5. **Hazır ayarları değiştirin**: **Glyphs** (Glifler) sekmesinde farklı görselleştirme stillerini keşfedin.

## 📖 Python betiği nasıl kullanılır?
Kullanımı oldukça basit ve anlaşılırdır. Yine de kurulumu, kullanımı, yapılandırma dosyalarını ayrıntılı olarak açıklayan ve bir sorun giderme bölümü içeren detaylı bir wiki sayfası hazırladık. Ayrıca nasıl yeni ön ayarlar oluşturabileceğinizi de öğrenebilirsiniz (gerçi henüz değil). [**musicViz.py**'nin bir python betiği olarak nasıl kullanılacağını görmek için buraya tıklayın](https://github.com/Aleks-Levet/better-nothing-music-visualizer/wiki/). Ne kadar havalı biliyor musunuz? Sınırsız sayıda dosyayı sorunsuz bir şekilde toplu olarak dönüştürebilirsiniz!

## <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Handshake.png" alt="Handshake" width="25" height="25" /> Topluluğumuza katılın
Konuşmak veya tartışmak mı istiyorsunuz? *Hatalar, özellik istekleri?* [**Nothing sunucusundaki resmi discord başlığına katılmaktan çekinmeyin!**](https://discord.com/channels/930878214237200394/1434923843239280743)

## 🏗️ Katkıda Bulunma
Gelin ve bize yardım edin! Katkılarınız çok makbule geçer!
Şunları yapabilirsiniz:
- Sorun (issue) açabilirsiniz
- Pull request gönderebilirsiniz
- İyileştirme önerilerinde bulunabilirsiniz
- Yeni görselleştirme fikirleri deneyebilirsiniz
- Yeni ön ayarlar oluşturabilirsiniz
- Geliştiricilerle tartışabilirsiniz

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f512/512.gif" alt="🔒" width="25" height="25"> Güvenlik
**VirusTotal taramasının bağlantısını burada bulabilirsiniz:**
https://www.virustotal.com/gui/url/c92c1ff82b56eb60bfd1e159592d09f949f0ea2d195e01f7f5adbef0e0b0385b?nocache=1

### <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Copyright.png" alt="Copyright" width="25" height="25" /> Emeği Geçenler:
#### İşte bu projede yer alan kişiler:
- [Aleks-Levet](https://github.com/Aleks-Levet) (Kurucu ve koordinatör, ana fikir, sahip)
- [Nicouschulas](https://github.com/Nicouschulas) (Beni oku (Readme) ve Viki (Wiki) geliştirmeleri)
- [rKyzen(a.k.a Shivank Dan)](https://github.com/rKyzen) (Gerçek zamanlı müzik akışı ile Android Uygulama geliştiricisi)
- [Oliver Lebaigue](https://github.com/oliver-lebaigue-bright-bench) (Geliştirici)
- [SebiAi](https://github.com/SebiAi) (Glif modlayıcı ve gliflerle ilgili yardım)
- [Earnedel-lab](https://github.com/Earendel-lab) (Beni oku (Readme) geliştirmeleri)
- [あけ なるかみ](https://github.com/Luke20YT) (Bu betikle entegre bir Müzik uygulaması yapan geliştirici)
- [Interlastic](https://github.com/Interlastic) (Betiği kolayca denemek için Discord Botu) (kullanımdan kaldırıldı)

<img alt="GitHub App Downloads" src="https://img.shields.io/github/downloads/Aleks-Levet/better-nothing-music-visualizer/total">

### <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Star.png" alt="Star" width="25" height="25" /> Yıldız Geçmişi
![Yıldız Geçmişi](https://api.star-history.com/svg?repos=Aleks-Levet/better-nothing-music-visualizer&type=Date)
