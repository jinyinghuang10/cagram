package org.telegram.ui.Components.Premium;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;

public class PremiumGradient {

    private final GradientTools mainGradient = new GradientTools(Theme.key_premiumGradient1, Theme.key_premiumGradient2, Theme.key_premiumGradient3, Theme.key_premiumGradient4);
    private final Paint mainGradientPaint = mainGradient.paint;
    Paint lockedPremiumPaint;

    private final static int size = 100;
    private final static int sizeHalf = 100 >> 1;

    private static PremiumGradient instance;

    public Drawable premiumStarColoredDrawable;
    public Drawable premiumStarDrawableMini;
    public InternalDrawable premiumStarMenuDrawable;
    public InternalDrawable premiumStarMenuDrawable2;

    private int lastStarColor;

    public static PremiumGradient getInstance() {
        if (instance == null) {
            instance = new PremiumGradient();
        }
        return instance;
    }

    private PremiumGradient() {
        premiumStarDrawableMini = ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_premium_liststar).mutate();
        premiumStarMenuDrawable = createGradientDrawable(ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_settings_premium));
        premiumStarMenuDrawable2 = createGradientDrawable(ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_premium_normal));
        premiumStarColoredDrawable = ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_premium_liststar).mutate();
        mainGradient.chekColors();
        checkIconColors();
    }

    public InternalDrawable createGradientDrawable(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getMinimumHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        mainGradient.paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mainGradient.gradientMatrix(0, 0, width, height, -width, 0);
        canvas.drawRect(0, 0, width, height, mainGradient.paint);
        mainGradient.paint.setXfermode(null);

        return new InternalDrawable(drawable, bitmap, mainGradient.colors);
    }

    public void checkIconColors() {
        if (Theme.getColor(Theme.key_chats_verifiedBackground) != lastStarColor) {
            lastStarColor = Theme.getColor(Theme.key_chats_verifiedBackground);
            premiumStarDrawableMini.setColorFilter(new PorterDuffColorFilter(lastStarColor, PorterDuff.Mode.MULTIPLY));
        }
        premiumStarMenuDrawable = checkColors(premiumStarMenuDrawable);
        premiumStarMenuDrawable2 = checkColors(premiumStarMenuDrawable2);
    }

    private InternalDrawable checkColors(InternalDrawable internalDrawable) {
        if (mainGradient.colors[0] != internalDrawable.colors[0] || mainGradient.colors[1] != internalDrawable.colors[1] || mainGradient.colors[2] != internalDrawable.colors[2] || mainGradient.colors[3] != internalDrawable.colors[3]) {
            return createGradientDrawable(internalDrawable.originDrawable);
        }
        return internalDrawable;
    }

    public void updateMainGradientMatrix(int x, int y, int width, int height, float xOffset, float yOffset) {
        mainGradient.gradientMatrix(x, y, width, height, xOffset, yOffset);
    }

    public static class InternalDrawable extends BitmapDrawable {

        public int[] colors;
        Drawable originDrawable;

        public InternalDrawable(Drawable originDrawable, Bitmap bitmap, int[] colors) {
            super(ApplicationLoader.applicationContext.getResources(), bitmap);
            this.originDrawable = originDrawable;
            this.colors = new int[colors.length];
            System.arraycopy(colors, 0, this.colors, 0, colors.length);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public void setColorFilter(int color, PorterDuff.Mode mode) {

        }
    }

    public Paint getMainGradientPaint() {
        if (MessagesController.getInstance(UserConfig.selectedAccount).premiumLocked) {
            if (lockedPremiumPaint == null) {
                lockedPremiumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            }
            lockedPremiumPaint.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            return lockedPremiumPaint;
        } else {
            return mainGradientPaint;
        }
    }
    public static class GradientTools {

        public float cx = 0.5f;
        public float cy = 0.5f;
        Shader shader;
        Matrix matrix = new Matrix();
        public final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        final String colorKey1, colorKey2, colorKey3, colorKey4, colorKey5;
        final int colors[] = new int[5];
        public boolean exactly;

        public float x1 = 0f, y1 = 1f, x2 = 1.5f, y2 = 0f;

        public GradientTools(String colorKey1, String colorKey2, String colorKey3, String colorKey4) {
            this(colorKey1, colorKey2, colorKey3, colorKey4, null);
        }

        public GradientTools(String colorKey1, String colorKey2, String colorKey3, String colorKey4, String colorKey5) {
            this.colorKey1 = colorKey1;
            this.colorKey2 = colorKey2;
            this.colorKey3 = colorKey3;
            this.colorKey4 = colorKey4;
            this.colorKey5 = colorKey5;
        }


        public void gradientMatrix(int x, int y, int x1, int y1, float xOffset, float yOffset) {
            chekColors();
            if (exactly) {
                int height = y1 - y;
                float sx = (x1 - x) / (float) size;
                float sy = height / (float) size;

                matrix.reset();
                matrix.postScale(sx, sy, 100 * cx, 100 * cy);
                matrix.postTranslate(xOffset, yOffset);
                shader.setLocalMatrix(matrix);
            } else {
                int height = y1 - y;
                int gradientHeight = height + height;
                float sx = (x1 - x) / (float) size;
                float sy = gradientHeight / (float) size;

                chekColors();
                matrix.reset();
                matrix.postScale(sx, sy, 75, sizeHalf);
                matrix.postTranslate(xOffset, -gradientHeight + yOffset);
                shader.setLocalMatrix(matrix);
            }
        }

        private void chekColors() {
            int c1 = Theme.getColor(colorKey1);
            int c2 = Theme.getColor(colorKey2);
            int c3 = colorKey3 == null ? 0 : Theme.getColor(colorKey3);
            int c4 = colorKey4 == null ? 0 : Theme.getColor(colorKey4);
            int c5 = colorKey5 == null ? 0 : Theme.getColor(colorKey5);
            if (colors[0] != c1 || colors[1] != c2 || colors[2] != c3 || colors[3] != c4 || colors[4] != c5) {
                colors[0] = c1;
                colors[1] = c2;
                colors[2] = c3;
                colors[3] = c4;
                colors[4] = c5;
                if (c3 == 0) {
                    shader = new LinearGradient(size * x1, size * y1, size * x2, size * y2, new int[]{colors[0], colors[1]}, new float[]{0, 1f}, Shader.TileMode.CLAMP);
                } else if (c4 == 0) {
                    shader = new LinearGradient(size * x1, size * y1, size * x2, size * y2, new int[]{colors[0], colors[1], colors[2]}, new float[]{0, 0.5f, 1f}, Shader.TileMode.CLAMP);
                } else if (c5 == 0) {
                    shader = new LinearGradient(size * x1, size * y1, size * x2, size * y2, new int[]{colors[0], colors[1], colors[2], colors[3]}, new float[]{0, 0.5f, 0.78f, 1f}, Shader.TileMode.CLAMP);
                } else {
                    shader = new LinearGradient(size * x1, size * y1, size * x2, size * y2, new int[]{colors[0], colors[1], colors[2], colors[3], colors[4]}, new float[]{0, 0.425f, 0.655f, 0.78f, 1f}, Shader.TileMode.CLAMP);
                }
                shader.setLocalMatrix(matrix);
                paint.setShader(shader);
            }
        }

        public void gradientMatrixLinear(float totalHeight, float offset) {
            chekColors();

            matrix.reset();
            matrix.postScale(1f, totalHeight / 100f, 0, 0);
            matrix.postTranslate(0, offset);
            shader.setLocalMatrix(matrix);
        }
    }
}
