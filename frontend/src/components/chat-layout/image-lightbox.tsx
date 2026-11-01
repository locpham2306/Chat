import { ChevronLeft, ChevronRight, Download, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Button } from '~/components/ui/button'

export interface LightboxImage {
  id: string
  name: string
  url: string
}

interface ImageLightboxProps {
  isOpen: boolean
  images: LightboxImage[]
  initialIndex?: number
  onClose: () => void
}

export const ImageLightbox = ({ isOpen, images, initialIndex = 0, onClose }: ImageLightboxProps) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex)
  const [prevInitialIndex, setPrevInitialIndex] = useState(initialIndex)

  if (prevInitialIndex !== initialIndex) {
    setPrevInitialIndex(initialIndex)
    setCurrentIndex(Math.max(0, Math.min(initialIndex, images.length - 1)))
  }

  const handlePrev = useCallback(() => {
    setCurrentIndex((prev) => (prev > 0 ? prev - 1 : images.length - 1))
  }, [images.length])

  const handleNext = useCallback(() => {
    setCurrentIndex((prev) => (prev < images.length - 1 ? prev + 1 : 0))
  }, [images.length])

  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      } else if (e.key === 'ArrowLeft' && images.length > 1) {
        handlePrev()
      } else if (e.key === 'ArrowRight' && images.length > 1) {
        handleNext()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, images.length, onClose, handlePrev, handleNext])

  if (!isOpen || images.length === 0) return null

  const currentImage = images[currentIndex] || images[0]

  return (
    <div
      role='dialog'
      aria-modal='true'
      aria-label='Xem ảnh phóng to'
      className='fixed inset-0 z-50 flex items-center justify-center bg-black/95 backdrop-blur-sm select-none'
      onClick={onClose}
    >
      {/* Top Bar / Actions */}
      <div
        className='absolute top-0 left-0 right-0 z-10 flex items-center justify-between p-3 sm:p-4 bg-gradient-to-b from-black/80 to-transparent'
        onClick={(e) => e.stopPropagation()}
      >
        <div className='flex items-center gap-3 min-w-0 pr-2'>
          <Button
            type='button'
            variant='ghost'
            size='icon'
            className='h-9 w-9 rounded-full bg-white/10 text-white hover:bg-white/20 hover:text-white shrink-0'
            onClick={onClose}
            aria-label='Đóng'
          >
            <X className='h-5 w-5' />
          </Button>
          <div className='min-w-0'>
            <p className='text-xs sm:text-sm font-medium text-white truncate'>{currentImage.name}</p>
            {images.length > 1 && (
              <p className='text-[11px] text-white/60'>
                {currentIndex + 1} / {images.length}
              </p>
            )}
          </div>
        </div>

        <div className='flex items-center gap-2 shrink-0'>
          <a
            href={currentImage.url}
            download={currentImage.name}
            className='inline-flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors'
            title='Tải ảnh xuống'
            aria-label='Tải ảnh xuống'
          >
            <Download className='h-4 w-4' />
          </a>
        </div>
      </div>

      {/* Navigation Buttons */}
      {images.length > 1 && (
        <>
          <Button
            type='button'
            variant='ghost'
            size='icon'
            className='absolute left-3 sm:left-6 z-10 h-10 w-10 sm:h-12 sm:w-12 rounded-full bg-white/10 text-white hover:bg-white/25 hover:text-white transition-all'
            onClick={(e) => {
              e.stopPropagation()
              handlePrev()
            }}
            aria-label='Ảnh trước'
          >
            <ChevronLeft className='h-6 w-6' />
          </Button>

          <Button
            type='button'
            variant='ghost'
            size='icon'
            className='absolute right-3 sm:right-6 z-10 h-10 w-10 sm:h-12 sm:w-12 rounded-full bg-white/10 text-white hover:bg-white/25 hover:text-white transition-all'
            onClick={(e) => {
              e.stopPropagation()
              handleNext()
            }}
            aria-label='Ảnh tiếp theo'
          >
            <ChevronRight className='h-6 w-6' />
          </Button>
        </>
      )}

      {/* Main Image Container */}
      <div className='relative max-h-[85vh] max-w-[90vw] flex items-center justify-center p-2'>
        <img
          src={currentImage.url}
          alt={currentImage.name}
          className='max-h-[85vh] max-w-[90vw] object-contain rounded-md shadow-2xl transition-transform duration-200 cursor-default'
          onClick={(e) => e.stopPropagation()}
        />
      </div>
    </div>
  )
}
