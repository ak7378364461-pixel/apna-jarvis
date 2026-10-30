# Apna JARVIS — Phase 1

यह हमारा अपना Android-native starter project है, किसी दूसरे JARVIS ऐप का APK नहीं।

## अभी क्या काम करता है
- हिंदी voice input (फोन के speech recognition engine पर निर्भर)
- हिंदी text-to-speech (फोन में उपलब्ध आवाज पर निर्भर)
- YouTube / WhatsApp खोलना
- Google / web search खोलना
- समय बताना

## अभी क्या शामिल नहीं है
- “Hey Jarvis” always-on wake word
- Screen-off background microphone service
- WhatsApp में अपने-आप message भेजना
- Cloud AI brain / memory
इनको permissions, privacy और battery व्यवहार जाँचकर अगले phases में जोड़ा जाएगा।

## Android फोन से APK बनाना
1. इस ZIP को GitHub पर एक नए repository में upload करें।
2. Repository का नाम `apna-jarvis` रखें और branch `main` रखें।
3. GitHub में Actions tab खोलें।
4. `Build Apna JARVIS APK` workflow चुनकर `Run workflow` दबाएँ।
5. Build पूरा होने पर Artifacts में `Apna-JARVIS-debug-apk` डाउनलोड करें।
6. ZIP extract करके `app-debug.apk` इंस्टॉल करें।

GitHub Actions build के लिए GitHub account और internet चाहिए। APK debug build है, production signing अभी नहीं की गई।
