# RedactGuard project-specific R8 rules belong here.
# Harness Consumer SDK rules must be shipped by the SDK itself.

# PdfBox-Android intentionally treats JP2/JPX decoding as optional. The
# com.gemalto.jp2 dependency is not bundled by default; when absent, JPX images
# are ignored by PdfBox-Android. R8 still resolves the optional reference at
# shrink time, so suppress only this known optional class instead of disabling
# shrinking or adding the legacy JP2 dependency.
-dontwarn com.gemalto.jp2.JP2Decoder
